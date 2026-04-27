#!/usr/bin/env python3
"""
Concurrent booking probe for /api/v1/citas/manual.

This script reuses the same payload contract from the existing booking script,
loading patients from CSV and firing concurrent requests for the exact same slot
(medicoId + fecha + hora) to validate optimistic concurrency behavior.
"""

from __future__ import annotations

import argparse
import csv
import json
import logging
import threading
import time
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from datetime import datetime, timedelta
from pathlib import Path
from typing import Any

import requests


DEFAULT_BASE_URL = "http://localhost:8080/api/v1"
DEFAULT_CSV_FILE = "pacientes.csv"
DEFAULT_TIMEOUT_SECONDS = 15
DEFAULT_WORKERS = 10
DEFAULT_ATTEMPTS = 10

HEADERS_JSON = {"Content-Type": "application/json"}

LOG = logging.getLogger("concurrency-citas")


@dataclass
class Slot:
    fecha: str
    hora: str
    intervalo_minutos: int | None = None


def configure_logging(output_dir: Path) -> tuple[Path, Path]:
    output_dir.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    log_file = output_dir / f"concurrency_{timestamp}.log"
    report_file = output_dir / f"concurrency_{timestamp}.json"

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)-8s %(message)s",
        datefmt="%H:%M:%S",
        handlers=[
            logging.StreamHandler(),
            logging.FileHandler(log_file, encoding="utf-8"),
        ],
    )
    return log_file, report_file


def post(base_url: str, path: str, payload: dict[str, Any], token: str | None, timeout: int) -> requests.Response:
    headers = {**HEADERS_JSON}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return requests.post(f"{base_url}{path}", headers=headers, json=payload, timeout=timeout)


def get(base_url: str, path: str, token: str | None, timeout: int) -> requests.Response:
    headers = {**HEADERS_JSON}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return requests.get(f"{base_url}{path}", headers=headers, timeout=timeout)


def login(base_url: str, username: str, password: str, timeout: int) -> str:
    response = post(
        base_url,
        "/auth/login",
        {"username": username, "password": password},
        token=None,
        timeout=timeout,
    )

    if response.status_code not in (200, 201):
        raise RuntimeError(f"Login failed for '{username}' -> HTTP {response.status_code}: {response.text[:180]}")

    token = response.json().get("token")
    if not token:
        raise RuntimeError("Login response does not contain token")
    return token


def load_patients(csv_file: Path, attempts: int) -> list[dict[str, str]]:
    if not csv_file.exists():
        raise FileNotFoundError(f"CSV file not found: {csv_file}")

    with csv_file.open(newline="", encoding="utf-8") as handle:
        rows = list(csv.DictReader(handle))

    required = {"documento", "nombres", "apellidos", "celular", "genero", "fechaNacimiento", "correo"}
    missing = required - set(rows[0].keys()) if rows else required
    if missing:
        raise ValueError(f"CSV missing required columns: {sorted(missing)}")

    if len(rows) < attempts:
        raise ValueError(f"CSV has {len(rows)} rows but attempts={attempts}")

    selected = rows[:attempts]
    LOG.info("Loaded %s patients from %s", len(selected), csv_file)
    return selected


def get_first_slot(base_url: str, token: str, medico_id: int, timeout: int) -> Slot:
    desde = (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%d")
    path = f"/citas/disponibilidad/primera?medicoId={medico_id}&desde={desde}"
    response = get(base_url, path, token=token, timeout=timeout)

    if response.status_code != 200:
        raise RuntimeError(
            f"Could not fetch first slot for medicoId={medico_id} -> "
            f"HTTP {response.status_code}: {response.text[:180]}"
        )

    payload = response.json()
    fecha = payload.get("fecha")
    hora = payload.get("hora")
    if not fecha or not hora:
        raise RuntimeError(f"Invalid slot payload: {payload}")

    return Slot(fecha=fecha, hora=hora, intervalo_minutos=payload.get("intervaloMinutos"))


def build_manual_payload(patient: dict[str, str], medico_id: int, slot: Slot, note: str) -> dict[str, Any]:
    return {
        "documento": patient["documento"],
        "nombres": patient["nombres"],
        "apellidos": patient["apellidos"],
        "celular": patient["celular"],
        "genero": patient["genero"],
        "fechaNacimiento": patient["fechaNacimiento"],
        "correo": patient["correo"],
        "medicoId": medico_id,
        "fecha": slot.fecha,
        "hora": slot.hora,
        "observaciones": note,
    }


def run_concurrent_booking(
    base_url: str,
    token: str,
    medico_id: int,
    slot: Slot,
    patients: list[dict[str, str]],
    workers: int,
    timeout: int,
) -> dict[str, Any]:
    barrier = threading.Barrier(len(patients))
    start_times: list[float] = []

    def worker(index: int, patient: dict[str, str]) -> dict[str, Any]:
        payload = build_manual_payload(
            patient,
            medico_id=medico_id,
            slot=slot,
            note=f"Concurrency probe request #{index}",
        )

        barrier.wait()
        request_started = time.perf_counter()
        response = post(base_url, "/citas/manual", payload, token=token, timeout=timeout)
        elapsed_ms = round((time.perf_counter() - request_started) * 1000, 2)

        try:
            body = response.json()
        except ValueError:
            body = response.text

        return {
            "attempt": index,
            "documento": patient.get("documento"),
            "status": response.status_code,
            "elapsedMs": elapsed_ms,
            "response": body,
        }

    results: list[dict[str, Any]] = []

    with ThreadPoolExecutor(max_workers=workers) as executor:
        futures = []
        for idx, patient in enumerate(patients, start=1):
            start_times.append(time.perf_counter())
            futures.append(executor.submit(worker, idx, patient))

        for future in as_completed(futures):
            try:
                result = future.result()
                results.append(result)
            except Exception as exc:  # pragma: no cover - defensive
                results.append({
                    "attempt": -1,
                    "documento": None,
                    "status": "EXCEPTION",
                    "elapsedMs": None,
                    "response": str(exc),
                })

    results.sort(key=lambda row: row["attempt"])

    status_counter = Counter(str(row["status"]) for row in results)
    success = sum(1 for row in results if row["status"] in (200, 201))

    return {
        "slot": {"fecha": slot.fecha, "hora": slot.hora, "medicoId": medico_id},
        "attempts": len(results),
        "successCount": success,
        "statusBreakdown": dict(status_counter),
        "results": results,
    }


def print_summary(report: dict[str, Any]) -> None:
    slot = report["slot"]
    LOG.info("=" * 72)
    LOG.info("Concurrency probe summary")
    LOG.info("Target slot: medicoId=%s | %s %s", slot["medicoId"], slot["fecha"], slot["hora"])
    LOG.info("Attempts: %s", report["attempts"])
    LOG.info("Success : %s", report["successCount"])
    LOG.info("Status breakdown: %s", report["statusBreakdown"])
    LOG.info("=" * 72)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Run concurrent /citas/manual requests against the same slot to probe optimistic concurrency behavior."
    )
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL, help="API base URL")
    parser.add_argument("--csv", default=DEFAULT_CSV_FILE, help="CSV file with registered patients")
    parser.add_argument("--username", required=True, help="User for /auth/login (MEDICO or AGENDADOR)")
    parser.add_argument("--password", required=True, help="Password for /auth/login")
    parser.add_argument("--medico-id", type=int, required=True, help="Target medicoId for concurrent booking")
    parser.add_argument("--attempts", type=int, default=DEFAULT_ATTEMPTS, help="How many concurrent booking attempts")
    parser.add_argument("--workers", type=int, default=DEFAULT_WORKERS, help="Thread pool size")
    parser.add_argument("--timeout", type=int, default=DEFAULT_TIMEOUT_SECONDS, help="HTTP timeout in seconds")
    parser.add_argument("--output-dir", default="scripts/reports", help="Directory for logs and JSON report")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    log_file, report_file = configure_logging(Path(args.output_dir))

    LOG.info("Starting concurrency probe")
    LOG.info("Base URL      : %s", args.base_url)
    LOG.info("CSV           : %s", args.csv)
    LOG.info("medicoId      : %s", args.medico_id)
    LOG.info("attempts/workers: %s/%s", args.attempts, args.workers)

    token = login(args.base_url, args.username, args.password, timeout=args.timeout)
    patients = load_patients(Path(args.csv), attempts=args.attempts)
    slot = get_first_slot(args.base_url, token, medico_id=args.medico_id, timeout=args.timeout)

    LOG.info("Using shared slot: %s %s", slot.fecha, slot.hora)

    report = run_concurrent_booking(
        base_url=args.base_url,
        token=token,
        medico_id=args.medico_id,
        slot=slot,
        patients=patients,
        workers=max(1, args.workers),
        timeout=args.timeout,
    )

    print_summary(report)

    report_payload = {
        "generatedAt": datetime.now().isoformat(),
        "baseUrl": args.base_url,
        "username": args.username,
        "medicoId": args.medico_id,
        "attempts": args.attempts,
        "workers": args.workers,
        "report": report,
        "logFile": str(log_file),
    }

    report_file.write_text(json.dumps(report_payload, ensure_ascii=False, indent=2), encoding="utf-8")
    LOG.info("Report written to: %s", report_file)
    LOG.info("Log written to   : %s", log_file)


if __name__ == "__main__":
    main()


import csv
import requests
import logging
from datetime import datetime
from pathlib import Path

# =============================================================
#  CONFIGURACION — ajusta estos valores antes de correr
# =============================================================
URL       = "http://localhost:8080/api/v1/auth/register/paciente"
CSV_FILE  = "pacientes.csv"                            # CSV en la misma carpeta
LOG_FILE  = f"resultados_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log"
HEADERS   = {"Content-Type": "application/json"}
# =============================================================

# ── Logger: consola + archivo ──────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s  %(levelname)-8s  %(message)s",
    datefmt="%H:%M:%S",
    handlers=[
        logging.StreamHandler(),
    ],
)
log = logging.getLogger(__name__)


def _normalizar_optional(valor):
    if valor is None:
        return None
    valor = str(valor).strip()
    return valor if valor else None


def registrar_pacientes():
    csv_path = Path(CSV_FILE)
    if not csv_path.exists():
        log.error(f"No se encontró el archivo: {CSV_FILE}")
        return

    exitosos   = []
    fallidos   = []

    log.info("=" * 60)
    log.info(f"  Inicio del proceso — {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}")
    log.info(f"  URL destino : {URL}")
    log.info(f"  Archivo CSV : {CSV_FILE}")
    log.info("=" * 60)

    with open(csv_path, newline="", encoding="utf-8") as f:
        reader = list(csv.DictReader(f))
        total  = len(reader)

        for i, row in enumerate(reader, start=1):
            correo = _normalizar_optional(row.get("correo"))
            payload = {
                "password"       : row.get("password"),
                "documento"      : row.get("documento"),
                "nombres"        : row.get("nombres"),
                "apellidos"      : row.get("apellidos"),
                "celular"        : row.get("celular"),
                "genero"         : row.get("genero"),
                "fechaNacimiento": row.get("fechaNacimiento"),
            }
            if correo:
                payload["correo"] = correo

            identificador = row.get("documento") or f"fila {i}"
            log.info(f"[{i:02d}/{total}] Registrando → {row.get('nombres')} {row.get('apellidos')} (doc: {identificador})")

            try:
                resp = requests.post(URL, headers=HEADERS, json=payload, timeout=10)

                if resp.status_code in (200, 201):
                    log.info(f"         ✅  Exito  |  HTTP {resp.status_code}  |  {resp.text[:120]}")
                    exitosos.append(identificador)
                else:
                    log.warning(f"         ⚠️  Fallo  |  HTTP {resp.status_code}  |  {resp.text[:120]}")
                    fallidos.append({"usuario": identificador, "status": resp.status_code, "respuesta": resp.text[:200]})

            except requests.exceptions.ConnectionError:
                log.error("         ❌  Error de conexion — verifica la URL y que el servidor este activo")
                fallidos.append({"usuario": identificador, "status": "CONNECTION_ERROR", "respuesta": "No se pudo conectar"})

            except requests.exceptions.Timeout:
                log.error("         ❌  Timeout — el servidor no respondio en 10 s")
                fallidos.append({"usuario": identificador, "status": "TIMEOUT", "respuesta": "Timeout"})

            except Exception as e:
                log.error(f"         ❌  Error inesperado: {e}")
                fallidos.append({"usuario": identificador, "status": "ERROR", "respuesta": str(e)})

    # ── Resumen final ──────────────────────────────────────
    log.info("")
    log.info("=" * 60)
    log.info("  RESUMEN FINAL")
    log.info("=" * 60)
    log.info(f"  Total procesados : {total}")
    log.info(f"  ✅ Exitosos       : {len(exitosos)}")
    log.info(f"  ❌ Fallidos       : {len(fallidos)}")

    if fallidos:
        log.info("")
        log.info("  Detalle de fallidos:")
        for f in fallidos:
            log.info(f"    • {f['usuario']}  →  {f['status']}  |  {f['respuesta'][:80]}")

    log.info("=" * 60)
    log.info(f"  Log guardado en: {LOG_FILE}")
    log.info("=" * 60)


if __name__ == "__main__":
    registrar_pacientes()

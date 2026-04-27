# Concurrency Probe Scripts

## `concurrency_citas_manual.py`

Stress test for concurrent booking against `POST /api/v1/citas/manual`.

What it does:

1. Logs in with the user you provide (`MEDICO` or `AGENDADOR`).
2. Loads patients from CSV (already registered in DB).
3. Fetches one shared slot from `GET /citas/disponibilidad/primera`.
4. Sends many concurrent `POST /citas/manual` calls using that exact same slot.
5. Writes a `.log` file and a `.json` report with status breakdown and per-attempt response.

## CSV columns expected

- `documento`
- `nombres`
- `apellidos`
- `celular`
- `genero`
- `fechaNacimiento`
- `correo`

## Quick run

```bash
python scripts/concurrency_citas_manual.py \
  --username medico.neural \
  --password Password123 \
  --medico-id 1 \
  --csv pacientes.csv \
  --attempts 12 \
  --workers 12
```

If your shell is `cmd.exe`, use:

```cmd
python scripts\concurrency_citas_manual.py --username medico.neural --password Password123 --medico-id 1 --csv pacientes.csv --attempts 12 --workers 12
```

## Output

Generated in `scripts/reports/`:

- `concurrency_YYYYMMDD_HHMMSS.log`
- `concurrency_YYYYMMDD_HHMMSS.json`

Use the JSON `statusBreakdown` to verify optimistic concurrency behavior (for example, 1 success + several business-rule conflicts).


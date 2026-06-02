import csv
import requests
import logging
from datetime import datetime, timedelta
from pathlib import Path

# =============================================================
#  CONFIGURACION — ajusta estos valores antes de correr
# =============================================================
BASE_URL  = "http://localhost:8080/api/v1"
CSV_FILE  = "pacientes.csv"                  # mismo CSV de pacientes
LOG_FILE  = f"citas_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log"

KEYCLOAK_BASE_URL = "http://localhost:8180"
KEYCLOAK_REALM = "piedrazul"
KEYCLOAK_CLIENT_ID = "piedrazul-frontend"
KEYCLOAK_CLIENT_SECRET = None
KEYCLOAK_SCOPE = "openid profile email roles"

ADMIN_USERNAME = "admin"
ADMIN_PASSWORD = "Password123!"
# =============================================================

HEADERS_JSON = {"Content-Type": "application/json"}

# ── Médicos a registrar ────────────────────────────────────
# Especialidades alineadas con EspecialidadMedica enum del backend:
# MEDICINA_GENERAL → CONSULTA_GENERAL
# TERAPIA_NEURAL   → TERAPIA_NEURAL
# QUIROPRAXIA      → QUIROPRAXIA
# FISIOTERAPIA     → FISIOTERAPIA
MEDICOS = [
    {
        "username"    : "medico.general",
        "password"    : "Password123!",
        "nombres"     : "Paola Andrea",
        "apellidos"   : "Ríos Castaño",
        "especialidad": "MEDICINA_GENERAL",
        "tipo"        : "MEDICO",
    },
    {
        "username"    : "medico.neural",
        "password"    : "Password123!",
        "nombres"     : "Clara Inés",
        "apellidos"   : "Córdoba Ruiz",
        "especialidad": "TERAPIA_NEURAL",
        "tipo"        : "MEDICO",
    },
    {
        "username"    : "medico.quiro",
        "password"    : "Password123!",
        "nombres"     : "Andrés Felipe",
        "apellidos"   : "Muñoz Lara",
        "especialidad": "QUIROPRAXIA",
        "tipo"        : "MEDICO",
    },
    {
        "username"    : "medico.fisio",
        "password"    : "Password123!",
        "nombres"     : "Laura Sofía",
        "apellidos"   : "Vargas Pinto",
        "especialidad": "FISIOTERAPIA",
        "tipo"        : "MEDICO",
    },
]

# ── Configuraciones variadas por médico ────────────────────
CONFIGURACIONES = [
    {
        "diasAtencion"    : ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
        "horaInicio"      : "07:00:00",
        "horaFin"         : "12:00:00",
        "intervaloMinutos": 15,
    },
    {
        "diasAtencion"    : ["MONDAY", "WEDNESDAY", "FRIDAY"],
        "horaInicio"      : "08:00:00",
        "horaFin"         : "14:00:00",
        "intervaloMinutos": 20,
    },
    {
        "diasAtencion"    : ["TUESDAY", "THURSDAY"],
        "horaInicio"      : "09:00:00",
        "horaFin"         : "17:00:00",
        "intervaloMinutos": 30,
    },
    {
        "diasAtencion"    : ["MONDAY", "TUESDAY", "FRIDAY"],
        "horaInicio"      : "07:00:00",
        "horaFin"         : "13:00:00",
        "intervaloMinutos": 30,
    },
]

# ── Logger ─────────────────────────────────────────────────
logging.basicConfig(
    level=logging.DEBUG,
    format="%(asctime)s  %(levelname)-8s  %(message)s",
    datefmt="%H:%M:%S",
    handlers=[
        logging.StreamHandler(),
    ],
)
log = logging.getLogger(__name__)


# ── Helpers HTTP ───────────────────────────────────────────
def post(url, payload, token=None):
    headers = {**HEADERS_JSON}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return requests.post(url, headers=headers, json=payload, timeout=10)

def put(url, payload, token=None):
    headers = {**HEADERS_JSON}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return requests.put(url, headers=headers, json=payload, timeout=10)

def get(url, token=None):
    headers = {**HEADERS_JSON}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return requests.get(url, headers=headers, timeout=10)

def separador(titulo=""):
    log.info("=" * 60)
    if titulo:
        log.info(f"  {titulo}")
        log.info("=" * 60)


def _keycloak_token(username, password, scope=KEYCLOAK_SCOPE):
    token_url = f"{KEYCLOAK_BASE_URL}/realms/{KEYCLOAK_REALM}/protocol/openid-connect/token"
    data = {
        "grant_type": "password",
        "client_id": KEYCLOAK_CLIENT_ID,
        "username": username,
        "password": password,
        "scope": scope,
    }
    if KEYCLOAK_CLIENT_SECRET:
        data["client_secret"] = KEYCLOAK_CLIENT_SECRET
    try:
        r = requests.post(token_url, data=data, timeout=10)
        if r.status_code == 200:
            return r.json().get("access_token")
        log.warning(f"  ⚠️  Keycloak token fallo para '{username}'  |  HTTP {r.status_code} — {r.text[:150]}")
        return None
    except Exception as e:
        log.error(f"  ❌  Error obteniendo token Keycloak para '{username}': {e}")
        return None


# ══════════════════════════════════════════════════════════════
#  PASO 1 — Obtener token de administrador (Keycloak)
# ══════════════════════════════════════════════════════════════
def obtener_admin_token():
    separador("PASO 1 — Token admin (Keycloak)")
    token = _keycloak_token(ADMIN_USERNAME, ADMIN_PASSWORD)
    if token:
        log.info("✅  Token de admin obtenido")
    return token


# ══════════════════════════════════════════════════════════════
#  PASO 2 — Registrar médicos
# ══════════════════════════════════════════════════════════════
def registrar_medicos(admin_token):
    separador("PASO 2 — Registrar médicos")
    for i, m in enumerate(MEDICOS):
        log.info(f"  [{i+1}/{len(MEDICOS)}] Registrando → {m['nombres']} {m['apellidos']} ({m['especialidad']})")
        try:
            r = post(f"{BASE_URL}/auth/register/medico", m, token=admin_token)
            if r.status_code in (200, 201):
                log.info(f"         ✅  Éxito  |  HTTP {r.status_code}")
            else:
                log.warning(f"         ⚠️  HTTP {r.status_code} — {r.text[:150]}")
        except Exception as e:
            log.error(f"         ❌  {e}")


# ══════════════════════════════════════════════════════════════
#  PASO 3 — Obtener medicoId real desde GET /medicos
# ══════════════════════════════════════════════════════════════
def obtener_medicos_reales(admin_token):
    """
    Consulta GET /medicos y cruza por nombre completo para obtener
    el medicoId real (distinto al userId del registro).
    """
    separador("PASO 3 — Obtener IDs reales de médicos")
    try:
        r = get(f"{BASE_URL}/medicos", token=admin_token)
        if r.status_code != 200:
            log.error(f"❌  No se pudo obtener lista de médicos  |  HTTP {r.status_code} — {r.text[:200]}")
            return [{**m, "medicoId": None} for m in MEDICOS]

        medicos_api = r.json()
        log.info(f"  API devolvió {len(medicos_api)} médico(s)")

        resultado = []
        for m_local in MEDICOS:
            nombre_completo = f"{m_local['nombres']} {m_local['apellidos']}"
            # Buscar por nombre completo primero, luego por especialidad como fallback
            match = next(
                (x for x in medicos_api
                 if x.get("nombresCompletos", "").strip() == nombre_completo.strip()),
                None
            ) or next(
                (x for x in medicos_api
                 if x.get("especialidad") == m_local["especialidad"]),
                None
            )

            if match:
                resultado.append({**m_local, "medicoId": match["id"]})
                log.info(f"  ✅  {nombre_completo} → medicoId={match['id']} ({match['especialidad']})")
            else:
                resultado.append({**m_local, "medicoId": None})
                log.warning(f"  ⚠️  No se encontró medicoId para {nombre_completo}")

        return resultado

    except Exception as e:
        log.error(f"❌  Error al obtener médicos: {e}")
        return [{**m, "medicoId": None} for m in MEDICOS]


# ══════════════════════════════════════════════════════════════
#  PASO 4 — Configurar horarios de médicos (token admin)
# ══════════════════════════════════════════════════════════════
def configurar_medicos(medicos, admin_token):
    separador("PASO 4 — Configurar horarios de médicos")
    for i, m in enumerate(medicos):
        mid = m.get("medicoId")
        if not mid:
            log.warning(f"  ⚠️  Sin medicoId para {m['username']}, omitiendo configuración")
            continue
        config = CONFIGURACIONES[i % len(CONFIGURACIONES)]
        log.info(f"  Configurando medicoId={mid} ({m['nombres']}) — días: {config['diasAtencion']} | intervalo: {config['intervaloMinutos']}min")
        try:
            r = put(f"{BASE_URL}/medicos/{mid}/configuracion", config, token=admin_token)
            if r.status_code in (200, 201):
                log.info(f"         ✅  Configuración aplicada")
            else:
                log.warning(f"         ⚠️  HTTP {r.status_code} — {r.text[:150]}")
        except Exception as e:
            log.error(f"         ❌  {e}")


# ══════════════════════════════════════════════════════════════
#  PASO 5 — Login de UN solo medico (token compartido)
# ══════════════════════════════════════════════════════════════
def login_medico_agendador(medicos):
    separador("PASO 5 — Login de medico agendador")
    for m in medicos:
        log.info(f"  Intentando login Keycloak → {m['username']}")
        token = _keycloak_token(m["username"], m["password"])
        if token:
            log.info(f"  ✅  Token obtenido para '{m['username']}' — se usara para todas las citas")
            return token
        log.warning("  ⚠️  Fallo, intentando con el siguiente medico...")
    log.error("❌  No se pudo obtener token de ningun medico")
    return None


# ══════════════════════════════════════════════════════════════
#  PASO 6 — Obtener primer slot disponible por médico
# ══════════════════════════════════════════════════════════════
# PASO 6 — Obtener primer slot disponible por médico
def obtener_slots(medicos, medico_token):
    separador("PASO 6 — Obtener primer slot disponible por médico")
    
    # Calcular la fecha de mañana en formato YYYY-MM-DD
    desde_manana = (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%d")
    
    for m in medicos:
        mid = m.get("medicoId")
        if not mid:
            m["slot"] = None
            continue
        try:
            # Agrégale a la URL: &desde=YYYY-MM-DD
            url = f"{BASE_URL}/citas/disponibilidad/primera?medicoId={mid}&desde={desde_manana}"
            r = get(url, token=medico_token)
            if r.status_code == 200:
                slot = r.json()
                m["slot"] = slot
                log.info(f"  ✅  medicoId={mid} ({m['nombres']}) → {slot.get('fecha')} {slot.get('hora')}")
            else:
                log.warning(f"  ⚠️  medicoId={mid} sin slot  |  HTTP {r.status_code} — {r.text[:100]}")
                m["slot"] = None
        except Exception as e:
            log.error(f"  ❌  {e}")
            m["slot"] = None
    return medicos



# ══════════════════════════════════════════════════════════════
#  PASO 7 — Agendar citas para los 30 pacientes
# ══════════════════════════════════════════════════════════════
def agendar_citas(medicos, pacientes, medico_token):
    separador("PASO 7 — Agendar citas para pacientes")

    medicos_activos = [m for m in medicos if m.get("slot") and m.get("medicoId")]
    if not medicos_activos:
        log.error("❌  Ningún médico con slot disponible. Abortando.")
        return

    # Contador independiente por médico para avanzar slots correctamente
    citas_por_medico = {m["medicoId"]: 0 for m in medicos_activos}

    exitosas, fallidas = [], []
    total = len(pacientes)

    for i, paciente in enumerate(pacientes):
        medico    = medicos_activos[i % len(medicos_activos)]
        mid       = medico["medicoId"]
        slot      = medico["slot"]
        intervalo = slot.get("intervaloMinutos", 15)
        n_cita    = citas_por_medico[mid]

        hora_dt = datetime.strptime(f"{slot['fecha']} {slot['hora']}", "%Y-%m-%d %H:%M:%S")
        hora_dt += timedelta(minutes=intervalo * n_cita)
        fecha_cita = hora_dt.strftime("%Y-%m-%d")
        hora_cita  = hora_dt.strftime("%H:%M:%S")
        citas_por_medico[mid] += 1

        # /citas/manual recibe datos del paciente directamente (no pacienteId).
        # Si el paciente ya existe en la BD la API ignora estos campos,
        # pero igual hay que enviarlos para evitar un 400.
        payload = {
            "documento"      : paciente["documento"],
            "nombres"        : paciente["nombres"],
            "apellidos"      : paciente["apellidos"],
            "celular"        : paciente["celular"],
            "genero"         : paciente["genero"],
            "fechaNacimiento": paciente["fechaNacimiento"],
            "correo"         : paciente["correo"],
            "medicoId"       : mid,
            "fecha"          : fecha_cita,
            "hora"           : hora_cita,
            "observaciones"  : "Cita agendada por script de pruebas",
        }

        log.info(f"  [{i+1:02d}/{total}] {paciente['nombres']} {paciente['apellidos']} "
                 f"→ Dr(a). {medico['nombres']} (medicoId={mid}) | {fecha_cita} {hora_cita}")

        try:
            r = post(f"{BASE_URL}/citas/manual", payload, token=medico_token)
            if r.status_code in (200, 201):
                log.info(f"           ✅  Cita agendada  |  HTTP {r.status_code}")
                exitosas.append(paciente.get("documento"))
            else:
                log.warning(f"           ⚠️  HTTP {r.status_code} — {r.text[:120]}")
                fallidas.append({"paciente": paciente.get("documento"), "status": r.status_code, "resp": r.text[:150]})
        except Exception as e:
            log.error(f"           ❌  {e}")
            fallidas.append({"paciente": paciente.get("documento"), "status": "ERROR", "resp": str(e)})

    separador("RESUMEN FINAL")
    log.info(f"  Total pacientes   : {total}")
    log.info(f"  ✅ Citas exitosas  : {len(exitosas)}")
    log.info(f"  ❌ Citas fallidas  : {len(fallidas)}")
    if fallidas:
        log.info("")
        log.info("  Detalle de fallidas:")
        for f in fallidas:
            log.info(f"    • {f['paciente']}  →  HTTP {f['status']}  |  {f['resp'][:80]}")
    log.info(f"\n  Log guardado en: {LOG_FILE}")
    separador()


# ══════════════════════════════════════════════════════════════
#  PASO 0 — Leer pacientes del CSV
# ══════════════════════════════════════════════════════════════
def leer_pacientes():
    """
    Lee el CSV de pacientes.
    Los campos usados en /citas/manual son:
    documento, nombres, apellidos, celular, genero, fechaNacimiento, correo.
    """
    csv_path = Path(CSV_FILE)
    if not csv_path.exists():
        log.error(f"No se encontro: {CSV_FILE}")
        return []

    with open(csv_path, newline="", encoding="utf-8") as f:
        pacientes = list(csv.DictReader(f))

    log.info(f"  {len(pacientes)} pacientes cargados del CSV")
    return pacientes


# ══════════════════════════════════════════════════════════════
#  MAIN
# ══════════════════════════════════════════════════════════════
def main():
    separador(f"INICIO — {datetime.now().strftime('%d/%m/%Y %H:%M:%S')}")
    log.info(f"  BASE_URL  : {BASE_URL}")
    log.info(f"  KEYCLOAK  : {KEYCLOAK_BASE_URL}/realms/{KEYCLOAK_REALM}")
    log.info(f"  CSV       : {CSV_FILE}")
    separador()

    pacientes = leer_pacientes()
    if not pacientes:
        return

    admin_token = obtener_admin_token()
    if not admin_token:
        log.error("❌  Sin token de admin. Abortando.")
        return

    registrar_medicos(admin_token)
    medicos      = obtener_medicos_reales(admin_token)
    configurar_medicos(medicos, admin_token)
    medico_token = login_medico_agendador(medicos)

    if not medico_token:
        log.error("❌  Sin token de medico. Abortando.")
        return

    medicos = obtener_slots(medicos, medico_token)
    agendar_citas(medicos, pacientes, medico_token)


if __name__ == "__main__":
    main()

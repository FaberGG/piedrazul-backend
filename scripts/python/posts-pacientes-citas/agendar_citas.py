import csv
import requests
import logging
import random
from datetime import datetime, timedelta
from pathlib import Path

# =============================================================
#  CONFIGURACION — ajusta estos valores antes de correr
# =============================================================
BASE_URL  = "http://localhost:8080/api/v1"
CSV_FILE  = "pacientes.csv"

KEYCLOAK_BASE_URL  = "http://localhost:8180"
KEYCLOAK_REALM     = "piedrazul"
KEYCLOAK_CLIENT_ID = "piedrazul-frontend"
KEYCLOAK_CLIENT_SECRET = None
KEYCLOAK_SCOPE     = "openid profile email roles"

ADMIN_USERNAME = "admin"
ADMIN_PASSWORD = "Password123!"

# Pacientes con CONSULTA_GENERAL que se marcarán ATENDIDA antes de agendar especialidades.
# Ajusta este número según cuántos pacientes quieres con historial.
N_ATENDIDAS = 20

# Ventana de fechas para cada fase (en días desde hoy)
FASE1_DESDE = 1   # CONSULTA_GENERAL: empieza mañana
FASE1_HASTA = 8   # ...durante 8 días hábiles
FASE3_DESDE = 5   # Especialidades: overlap con fase 1 para que coincidan en el mismo día
FASE3_HASTA = 14
# =============================================================

HEADERS_JSON = {"Content-Type": "application/json"}

# ── Médicos ────────────────────────────────────────────────
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

# ── Horarios por médico (índice = posición en MEDICOS) ─────
CONFIGURACIONES = [
    {   # MEDICINA_GENERAL — lunes a viernes, mañana
        "diasAtencion"    : ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
        "horaInicio"      : "07:00:00",
        "horaFin"         : "12:00:00",
        "intervaloMinutos": 15,
    },
    {   # TERAPIA_NEURAL — lun/mié/vie, mañana-tarde
        "diasAtencion"    : ["MONDAY", "WEDNESDAY", "FRIDAY"],
        "horaInicio"      : "08:00:00",
        "horaFin"         : "14:00:00",
        "intervaloMinutos": 20,
    },
    {   # QUIROPRAXIA — mar/jue, jornada larga
        "diasAtencion"    : ["TUESDAY", "THURSDAY"],
        "horaInicio"      : "09:00:00",
        "horaFin"         : "17:00:00",
        "intervaloMinutos": 30,
    },
    {   # FISIOTERAPIA — lun/mar/vie, mañana
        "diasAtencion"    : ["MONDAY", "TUESDAY", "FRIDAY"],
        "horaInicio"      : "07:00:00",
        "horaFin"         : "13:00:00",
        "intervaloMinutos": 30,
    },
]

# ── Observaciones realistas por especialidad ───────────────
OBSERVACIONES = {
    "MEDICINA_GENERAL": [
        "Paciente refiere dolor lumbar de larga data, solicita valoración inicial.",
        "Consulta general por fatiga crónica y cefalea recurrente.",
        "Primera consulta: evaluación de hipertensión arterial leve.",
        "Paciente con antecedentes de gastritis, solicita seguimiento.",
        "Control de peso y evaluación nutricional básica.",
        "Consulta por insomnio y ansiedad moderada.",
        "Valoración inicial por dolor articular en rodillas.",
        "Paciente refiere mareos frecuentes y pérdida de apetito.",
        "Primera visita: evaluación de síntomas digestivos.",
        "Consulta preventiva y chequeo general.",
    ],
    "TERAPIA_NEURAL": [
        "Seguimiento de terapia neural por dolor cervical crónico.",
        "Paciente con neuralgia intercostal, indicado bloqueo nervioso.",
        "Tratamiento de migraña refractaria mediante terapia neural segmentaria.",
        "Control post-terapia neural por ciática derecha.",
        "Terapia neural focal por dolor de hombro con irradiación al brazo.",
        "Paciente con síndrome de túnel carpiano bilateral, terapia neural.",
        "Dolor facial crónico atípico, evaluación y aplicación neural.",
    ],
    "QUIROPRAXIA": [
        "Ajuste quiropráctico lumbar por hernia discal L4-L5.",
        "Sesión de manipulación cervical por tortícolis recurrente.",
        "Tratamiento de escoliosis leve mediante técnica quiropráctica.",
        "Control quiropráctico post-accidente de tránsito.",
        "Ajuste de columna dorsal por contractura muscular intensa.",
        "Paciente con espondilosis cervical, sesión de quiropraxia.",
        "Manipulación de sacroilíaca por dolor pélvico crónico.",
    ],
    "FISIOTERAPIA": [
        "Rehabilitación funcional de rodilla derecha post-meniscectomía.",
        "Fisioterapia por tendinitis del manguito rotador izquierdo.",
        "Sesión de electroterapia y ultrasonido por esguince de tobillo.",
        "Fortalecimiento de core y reeducación postural.",
        "Rehabilitación de hombro por luxación recurrente.",
        "Fisioterapia respiratoria por EPOC leve.",
        "Control de fisioterapia por fractura de muñeca consolidada.",
    ],
}

# ── Logger ─────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s  %(levelname)-8s  %(message)s",
    datefmt="%H:%M:%S",
    handlers=[logging.StreamHandler()],
)
log = logging.getLogger(__name__)


# ══════════════════════════════════════════════════════════════
#  Helpers HTTP
# ══════════════════════════════════════════════════════════════
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

def patch(url, payload, token=None):
    headers = {**HEADERS_JSON}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return requests.patch(url, headers=headers, json=payload, timeout=10)

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

def obs(especialidad):
    return random.choice(OBSERVACIONES.get(especialidad, ["Consulta de control."]))


# ══════════════════════════════════════════════════════════════
#  Tokens Keycloak
# ══════════════════════════════════════════════════════════════
def _keycloak_token(username, password):
    token_url = f"{KEYCLOAK_BASE_URL}/realms/{KEYCLOAK_REALM}/protocol/openid-connect/token"
    data = {
        "grant_type": "password",
        "client_id" : KEYCLOAK_CLIENT_ID,
        "username"  : username,
        "password"  : password,
        "scope"     : KEYCLOAK_SCOPE,
    }
    if KEYCLOAK_CLIENT_SECRET:
        data["client_secret"] = KEYCLOAK_CLIENT_SECRET
    try:
        r = requests.post(token_url, data=data, timeout=10)
        if r.status_code == 200:
            return r.json().get("access_token")
        log.warning(f"  ⚠️  Token fallo para '{username}'  |  HTTP {r.status_code}")
        return None
    except Exception as e:
        log.error(f"  ❌  Error token '{username}': {e}")
        return None


# ══════════════════════════════════════════════════════════════
#  PASO 1 — Token admin
# ══════════════════════════════════════════════════════════════
def obtener_admin_token():
    separador("PASO 1 — Token admin (Keycloak)")
    token = _keycloak_token(ADMIN_USERNAME, ADMIN_PASSWORD)
    if token:
        log.info("  ✅  Token de admin obtenido")
    return token


# ══════════════════════════════════════════════════════════════
#  PASO 2 — Registrar médicos
# ══════════════════════════════════════════════════════════════
def registrar_medicos(admin_token):
    separador("PASO 2 — Registrar médicos")
    for i, m in enumerate(MEDICOS):
        log.info(f"  [{i+1}/{len(MEDICOS)}] {m['nombres']} {m['apellidos']} ({m['especialidad']})")
        try:
            r = post(f"{BASE_URL}/auth/register/medico", m, token=admin_token)
            if r.status_code in (200, 201):
                log.info(f"         ✅  HTTP {r.status_code}")
            else:
                log.warning(f"         ⚠️  HTTP {r.status_code} — {r.text[:150]}")
        except Exception as e:
            log.error(f"         ❌  {e}")


# ══════════════════════════════════════════════════════════════
#  PASO 3 — Obtener IDs reales de médicos
# ══════════════════════════════════════════════════════════════
def obtener_medicos_reales(admin_token):
    separador("PASO 3 — Obtener IDs reales de médicos")
    try:
        r = get(f"{BASE_URL}/medicos", token=admin_token)
        if r.status_code != 200:
            log.error(f"  ❌  HTTP {r.status_code} — {r.text[:200]}")
            return [{**m, "medicoId": None} for m in MEDICOS]

        medicos_api = r.json()
        log.info(f"  API devolvió {len(medicos_api)} médico(s)")

        resultado = []
        for m_local in MEDICOS:
            nombre = f"{m_local['nombres']} {m_local['apellidos']}"
            match = next(
                (x for x in medicos_api if x.get("nombresCompletos", "").strip() == nombre.strip()),
                None,
            ) or next(
                (x for x in medicos_api if x.get("especialidad") == m_local["especialidad"]),
                None,
            )
            if match:
                resultado.append({**m_local, "medicoId": match["id"]})
                log.info(f"  ✅  {nombre} → medicoId={match['id']} ({match['especialidad']})")
            else:
                resultado.append({**m_local, "medicoId": None})
                log.warning(f"  ⚠️  Sin medicoId para {nombre}")

        return resultado
    except Exception as e:
        log.error(f"  ❌  {e}")
        return [{**m, "medicoId": None} for m in MEDICOS]


# ══════════════════════════════════════════════════════════════
#  PASO 4 — Configurar horarios
# ══════════════════════════════════════════════════════════════
def configurar_medicos(medicos, admin_token):
    separador("PASO 4 — Configurar horarios de médicos")
    for i, m in enumerate(medicos):
        mid = m.get("medicoId")
        if not mid:
            log.warning(f"  ⚠️  Sin medicoId para {m['username']}, omitiendo")
            continue
        config = CONFIGURACIONES[i % len(CONFIGURACIONES)]
        m["config"] = config
        log.info(f"  medicoId={mid} ({m['nombres']}) — {config['diasAtencion']} | {config['intervaloMinutos']}min")
        try:
            r = put(f"{BASE_URL}/medicos/{mid}/configuracion", config, token=admin_token)
            if r.status_code in (200, 201):
                log.info(f"         ✅  Configuración aplicada")
            else:
                log.warning(f"         ⚠️  HTTP {r.status_code} — {r.text[:150]}")
        except Exception as e:
            log.error(f"         ❌  {e}")


# ══════════════════════════════════════════════════════════════
#  PASO 5 — Login médico (token compartido para crear citas)
# ══════════════════════════════════════════════════════════════
def login_medico(medicos):
    separador("PASO 5 — Login de médico agendador")
    for m in medicos:
        token = _keycloak_token(m["username"], m["password"])
        if token:
            log.info(f"  ✅  Token obtenido para '{m['username']}'")
            return token
        log.warning(f"  ⚠️  Fallo con '{m['username']}', intentando siguiente...")
    log.error("  ❌  No se pudo obtener token de ningún médico")
    return None


# ══════════════════════════════════════════════════════════════
#  Generador de pool de slots locales
# ══════════════════════════════════════════════════════════════
def generar_pool_slots(medicos, desde_dias, hasta_dias):
    """
    Calcula todos los slots disponibles para cada médico en la ventana
    [hoy+desde_dias, hoy+hasta_dias] basado en su configuración local.
    Retorna lista de dicts {medicoId, especialidad, nombres, fecha, hora}.
    Agrupa por fecha para visualizar días con múltiples médicos.
    """
    pool = []
    hoy = datetime.now().date()

    for m in medicos:
        config = m.get("config")
        mid = m.get("medicoId")
        if not config or not mid:
            continue

        dias_activos = set(config["diasAtencion"])
        hora_inicio  = datetime.strptime(config["horaInicio"], "%H:%M:%S").time()
        hora_fin     = datetime.strptime(config["horaFin"], "%H:%M:%S").time()
        intervalo    = config["intervaloMinutos"]

        for delta in range(desde_dias, hasta_dias + 1):
            fecha = hoy + timedelta(days=delta)
            if fecha.strftime("%A").upper() not in dias_activos:
                continue
            slot_dt = datetime.combine(fecha, hora_inicio)
            fin_dt  = datetime.combine(fecha, hora_fin)
            while slot_dt < fin_dt:
                pool.append({
                    "medicoId"  : mid,
                    "especialidad": m["especialidad"],
                    "nombres"   : m["nombres"],
                    "fecha"     : fecha.strftime("%Y-%m-%d"),
                    "hora"      : slot_dt.strftime("%H:%M:%S"),
                })
                slot_dt += timedelta(minutes=intervalo)

    return pool

def _log_medicos_por_dia(pool):
    """Muestra qué médicos estarán disponibles en cada día del pool."""
    por_dia = {}
    for s in pool:
        por_dia.setdefault(s["fecha"], set()).add(s["especialidad"])
    for fecha in sorted(por_dia):
        especialidades = ", ".join(sorted(por_dia[fecha]))
        log.info(f"    {fecha}: {especialidades}")


# ══════════════════════════════════════════════════════════════
#  FASE 1 — Agendar CONSULTA_GENERAL para todos los pacientes
# ══════════════════════════════════════════════════════════════
def fase1_consultas_generales(medicos, pacientes, medico_token):
    separador("FASE 1 — CONSULTA_GENERAL para 30 pacientes (próximos 8 días hábiles)")

    medico_general = next((m for m in medicos if m["especialidad"] == "MEDICINA_GENERAL"), None)
    if not medico_general or not medico_general.get("medicoId"):
        log.error("  ❌  No hay médico general disponible. Abortando fase 1.")
        return []

    pool = generar_pool_slots([medico_general], FASE1_DESDE, FASE1_HASTA)
    log.info(f"  Pool generado: {len(pool)} slots para MEDICINA_GENERAL")
    _log_medicos_por_dia(pool)

    # Seleccionar slots espaciados: máximo 4 pacientes por día
    slots_por_dia = {}
    for s in pool:
        slots_por_dia.setdefault(s["fecha"], []).append(s)

    slots_seleccionados = []
    MAX_POR_DIA = 4
    for fecha in sorted(slots_por_dia):
        candidatos = slots_por_dia[fecha]
        step = max(1, len(candidatos) // MAX_POR_DIA)
        slots_seleccionados.extend(candidatos[i] for i in range(0, min(len(candidatos), MAX_POR_DIA * step), step))

    if len(slots_seleccionados) < len(pacientes):
        log.warning(f"  ⚠️  Solo {len(slots_seleccionados)} slots en la ventana; hay {len(pacientes)} pacientes.")

    citas_creadas = []  # [{citaId, pacienteDoc}]
    total = len(pacientes)
    mid = medico_general["medicoId"]

    for i, paciente in enumerate(pacientes):
        if i >= len(slots_seleccionados):
            log.warning(f"  ⚠️  Sin slot para paciente {i+1}/{total} — extendiendo último día")
            last = slots_seleccionados[-1]
            extra_dt = datetime.strptime(f"{last['fecha']} {last['hora']}", "%Y-%m-%d %H:%M:%S")
            extra_dt += timedelta(minutes=medico_general["config"]["intervaloMinutos"] * (i - len(slots_seleccionados) + 1))
            slot = {"fecha": extra_dt.strftime("%Y-%m-%d"), "hora": extra_dt.strftime("%H:%M:%S")}
        else:
            slot = slots_seleccionados[i]

        payload = {
            "documento"      : paciente["documento"],
            "nombres"        : paciente["nombres"],
            "apellidos"      : paciente["apellidos"],
            "celular"        : paciente["celular"],
            "genero"         : paciente["genero"],
            "fechaNacimiento": paciente["fechaNacimiento"],
            "correo"         : paciente["correo"],
            "medicoId"       : mid,
            "fecha"          : slot["fecha"],
            "hora"           : slot["hora"],
            "observaciones"  : obs("MEDICINA_GENERAL"),
        }

        log.info(f"  [{i+1:02d}/{total}] {paciente['nombres']} {paciente['apellidos']} "
                 f"→ Dr(a). {medico_general['nombres']} | {slot['fecha']} {slot['hora']}")

        try:
            r = post(f"{BASE_URL}/citas/manual", payload, token=medico_token)
            if r.status_code in (200, 201):
                cita_id = r.json().get("id") or r.json().get("citaId")
                citas_creadas.append({"citaId": cita_id, "doc": paciente["documento"], "nombre": f"{paciente['nombres']} {paciente['apellidos']}"})
                log.info(f"           ✅  citaId={cita_id}")
            else:
                log.warning(f"           ⚠️  HTTP {r.status_code} — {r.text[:120]}")
        except Exception as e:
            log.error(f"           ❌  {e}")

    log.info(f"\n  Fase 1 completa: {len(citas_creadas)}/{total} citas creadas")
    return citas_creadas


# ══════════════════════════════════════════════════════════════
#  FASE 2 — Marcar N_ATENDIDAS citas como ATENDIDA
# ══════════════════════════════════════════════════════════════
def fase2_marcar_atendidas(citas_creadas, medico_token):
    separador(f"FASE 2 — Marcar {N_ATENDIDAS} consultas generales como ATENDIDA")

    candidatas = [c for c in citas_creadas if c.get("citaId")]
    a_atender  = candidatas[:N_ATENDIDAS]

    atendidas_doc = set()
    for c in a_atender:
        cid = c["citaId"]
        log.info(f"  PATCH /citas/{cid} → ATENDIDA ({c['nombre']})")
        try:
            r = patch(
                f"{BASE_URL}/citas/{cid}",
                {"nuevoEstado": "ATENDIDA", "nuevasObservaciones": "Consulta realizada satisfactoriamente."},
                token=medico_token,
            )
            if r.status_code in (200, 201):
                atendidas_doc.add(c["doc"])
                log.info(f"         ✅  HTTP {r.status_code}")
            else:
                log.warning(f"         ⚠️  HTTP {r.status_code} — {r.text[:120]}")
        except Exception as e:
            log.error(f"         ❌  {e}")

    log.info(f"\n  Fase 2 completa: {len(atendidas_doc)} pacientes con CONSULTA_GENERAL ATENDIDA")
    return atendidas_doc  # set de documentos de pacientes elegibles para especialidades


# ══════════════════════════════════════════════════════════════
#  FASE 3 — Citas de especialidad (solo pacientes con CG atendida)
# ══════════════════════════════════════════════════════════════
def fase3_especialidades(medicos, pacientes, atendidas_doc, medico_token):
    separador("FASE 3 — Citas de especialidad (días +5 a +14, múltiples médicos por día)")

    especialistas = [m for m in medicos if m["especialidad"] != "MEDICINA_GENERAL" and m.get("medicoId")]
    if not especialistas:
        log.error("  ❌  Sin médicos especialistas disponibles")
        return

    # Pool de slots para especialistas en la ventana FASE3
    pool = generar_pool_slots(especialistas, FASE3_DESDE, FASE3_HASTA)
    log.info(f"  Pool generado: {len(pool)} slots para especialistas")
    log.info("  Días con médicos disponibles:")
    _log_medicos_por_dia(pool)

    # Pacientes elegibles (con CONSULTA_GENERAL ATENDIDA)
    elegibles = [p for p in pacientes if p["documento"] in atendidas_doc]
    log.info(f"\n  Pacientes elegibles: {len(elegibles)}")

    if not elegibles:
        log.warning("  ⚠️  Ningún paciente elegible. Verifica que la fase 2 marcó citas como ATENDIDA.")
        return

    # Distribuir pacientes entre especialistas, rotando por especialidad
    # Agrupar slots por especialidad para asignar equitativamente
    slots_por_esp = {}
    for s in pool:
        slots_por_esp.setdefault(s["especialidad"], []).append(s)

    # Seleccionar max 3 slots por día por especialidad
    def seleccionar_slots_esp(slots_lista, max_por_dia=3):
        por_dia = {}
        for s in slots_lista:
            por_dia.setdefault(s["fecha"], []).append(s)
        resultado = []
        for fecha in sorted(por_dia):
            candidatos = por_dia[fecha]
            step = max(1, len(candidatos) // max_por_dia)
            resultado.extend(candidatos[i] for i in range(0, min(len(candidatos), max_por_dia * step), step))
        return resultado

    especialidades_en_orden = [m["especialidad"] for m in especialistas]
    slots_disponibles = []
    for esp in especialidades_en_orden:
        slots_disponibles.extend(seleccionar_slots_esp(slots_por_esp.get(esp, [])))

    # Shuffle para que los días con múltiples médicos se intercalen naturalmente
    random.shuffle(slots_disponibles)

    exitosas, fallidas = [], []
    total = len(elegibles)

    for i, paciente in enumerate(elegibles):
        if i >= len(slots_disponibles):
            log.warning(f"  ⚠️  Sin slots para paciente {i+1} — omitiendo")
            fallidas.append(paciente["documento"])
            continue

        slot = slots_disponibles[i]
        mid  = slot["medicoId"]
        especialidad = slot["especialidad"]

        payload = {
            "documento"      : paciente["documento"],
            "nombres"        : paciente["nombres"],
            "apellidos"      : paciente["apellidos"],
            "celular"        : paciente["celular"],
            "genero"         : paciente["genero"],
            "fechaNacimiento": paciente["fechaNacimiento"],
            "correo"         : paciente["correo"],
            "medicoId"       : mid,
            "fecha"          : slot["fecha"],
            "hora"           : slot["hora"],
            "observaciones"  : obs(especialidad),
        }

        log.info(f"  [{i+1:02d}/{total}] {paciente['nombres']} {paciente['apellidos']} "
                 f"→ {slot['nombres']} ({especialidad}) | {slot['fecha']} {slot['hora']}")

        try:
            r = post(f"{BASE_URL}/citas/manual", payload, token=medico_token)
            if r.status_code in (200, 201):
                log.info(f"           ✅  HTTP {r.status_code}")
                exitosas.append(paciente["documento"])
            else:
                log.warning(f"           ⚠️  HTTP {r.status_code} — {r.text[:120]}")
                fallidas.append(paciente["documento"])
        except Exception as e:
            log.error(f"           ❌  {e}")
            fallidas.append(paciente["documento"])

    separador("RESUMEN FASE 3")
    log.info(f"  Total elegibles      : {total}")
    log.info(f"  ✅ Citas agendadas   : {len(exitosas)}")
    log.info(f"  ❌ Citas fallidas    : {len(fallidas)}")


# ══════════════════════════════════════════════════════════════
#  PASO 0 — Leer pacientes del CSV
# ══════════════════════════════════════════════════════════════
def leer_pacientes():
    csv_path = Path(CSV_FILE)
    if not csv_path.exists():
        log.error(f"  No se encontró: {CSV_FILE}")
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
        log.error("  ❌  Sin token de admin. Abortando.")
        return

    registrar_medicos(admin_token)
    medicos = obtener_medicos_reales(admin_token)
    configurar_medicos(medicos, admin_token)

    medico_token = login_medico(medicos)
    if not medico_token:
        log.error("  ❌  Sin token de médico. Abortando.")
        return

    # Fase 1: CONSULTA_GENERAL para todos
    citas_creadas = fase1_consultas_generales(medicos, pacientes, medico_token)

    # Fase 2: Marcar N_ATENDIDAS como ATENDIDA
    atendidas_doc = fase2_marcar_atendidas(citas_creadas, medico_token)

    # Fase 3: Especialidades para pacientes con CG atendida
    fase3_especialidades(medicos, pacientes, atendidas_doc, medico_token)

    separador("PROCESO COMPLETO")


if __name__ == "__main__":
    main()

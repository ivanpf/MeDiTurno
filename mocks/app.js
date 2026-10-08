// Shell: <body data-role="paciente|funcionario|medico|admin|public" data-active="Texto del menu" data-cu="CU-X" data-title="...">
// ===== GLOBAL: poné false para ocultar todos los botones "Simular" en todos los mocks =====
const SHOW_SIMULAR = false;
if (!SHOW_SIMULAR) document.write('<style>.scenarios{display:none!important}</style>');

const MENUS = {
  paciente: { label: 'Paciente', items: [['Inicio', 'cu08.html'], ['Reservar consulta', 'cu08.html'], ['Mis reservas', 'cu09.html'], ['Historia clínica', 'cu11.html'], ['Anuncios', '#']] },
  funcionario: { label: 'Funcionario', items: [['Turnos semanales', 'cu20.html'], ['Alta de turno', 'cu12.html'], ['Turnos suspendidos', 'cu14.html'], ['Consulta de reserva', 'cu15.html'], ['Anuncios', '#']] },
  medico: { label: 'Médico', items: [['Mis pacientes', 'cu21.html'], ['Historia clínica', 'cu22.html'], ['Registrar consulta', 'cu23.html'], ['Emitir pase', 'cu24.html'], ['Habilitar reconsulta', 'cu25.html']] },
  admin: { label: 'Administrador', items: [['Alta de especialidades', 'cu28.html'], ['Alta de pacientes', 'cu31.html'], ['Alta de usuarios internos', 'cu34.html'], ['Alta de consultorios', 'cu37.html'], ['Estadísticas', '#'], ['Cargas masivas', '#']] },
};
document.addEventListener('DOMContentLoaded', () => {
  const b = document.body, role = b.dataset.role || 'public', cu = b.dataset.cu || '', title = b.dataset.title || '';
  const content = b.innerHTML;
  const m = MENUS[role];
  let html = `<div class="top"><span><b>MeDiTurno</b> <span class="muted" style="color:#ccc">— mock ${cu} ${title}</span></span><span>`;
  html += m ? `${m.label} · Juan Pérez · <a href="cu02.html">Cerrar sesión</a>` : '<a href="index.html">← índice</a>';
  html += `</span></div>`;
  if (m) {
    html += `<div class="layout"><div class="side"><h4>${m.label}</h4>` +
      m.items.map(([t, h]) => `<a href="${h}" class="${t === b.dataset.active ? 'on' : ''}">${t}</a>`).join('') +
      `<h4>Mock</h4><a href="index.html">← índice de mocks</a></div><div class="main">${content}</div></div>`;
  } else {
    html += content;
  }
  b.innerHTML = html;
});
// helpers
function show(id) { document.querySelectorAll('[data-state]').forEach(e => e.classList.add('hide')); document.getElementById(id)?.classList.remove('hide'); }
function toggle(id, on) { document.getElementById(id)?.classList.toggle('hide', !on); }

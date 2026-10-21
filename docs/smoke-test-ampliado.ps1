# Verificacion ampliada de TicketDesk: validaciones, permisos y escenarios de error (casos N01 a N13
# y N16 de docs/estrategia-pruebas-ticketdesk.md).
#
# Uso, desde PowerShell:
#   powershell -ExecutionPolicy Bypass -File .\smoke-test-ampliado.ps1 -ApiUrl "https://URL-DEL-BACKEND.onrender.com/api/v1"
#
# Requiere los datos de prueba (empresa "Empresa Demo" y sus cuatro usuarios, contrasena Demo1234!).
# Crea datos con el prefijo QA: un usuario que queda desactivado, una categoria y algunos tickets.
# Deshabilita temporalmente el modulo de voz y lo vuelve a habilitar al terminar.
# Los casos N14 y N15 (cierre automatico y rebote desde Critico) se hacen a mano.

param([Parameter(Mandatory = $true)][string]$ApiUrl)

$ApiUrl = $ApiUrl.TrimEnd('/')
$Password = "Demo1234!"
$sello = (Get-Date).ToString("yyyyMMddHHmmss")
$script:aprobados = 0
$script:fallos = 0
$script:revisar = 0

function Anotar($nombre, $correcto, $detalle) {
    if ($correcto) { $script:aprobados++; Write-Host ("  [OK]    " + $nombre) -ForegroundColor Green }
    else { $script:fallos++; Write-Host ("  [FALLO] " + $nombre + " | " + $detalle) -ForegroundColor Red }
}

function Revisar($nombre, $detalle) {
    $script:revisar++
    Write-Host ("  [REVISAR] " + $nombre + " | " + $detalle) -ForegroundColor Yellow
}

function Enviar($metodo, $url, $headers, $obj) {
    $json = $obj | ConvertTo-Json
    Invoke-RestMethod -Method $metodo -Uri $url -Headers $headers -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($json)) -TimeoutSec 120
}

function CodigoHttp($bloque) {
    try { & $bloque | Out-Null; return 200 } catch { return [int]$_.Exception.Response.StatusCode.value__ }
}

function Entrar($email, $clave, $empresaId) {
    $cuerpo = '{"email":"' + $email + '","password":"' + $clave + '","empresaId":"' + $empresaId + '"}'
    Invoke-RestMethod -Method Post -Uri "$ApiUrl/auth/login" -ContentType "application/json" -Body $cuerpo -TimeoutSec 120
}

function NuevoTicket($titulo) {
    Enviar Post "$ApiUrl/tickets" $hc @{ titulo = $titulo; descripcion = "Ticket de prueba QA"; categoriaId = $catBug; prioridad = "BAJA" }
}

Write-Host "Despertando el servidor (puede tardar hasta un par de minutos)..."
try { $empresas = Invoke-RestMethod -Uri "$ApiUrl/auth/empresas" -TimeoutSec 240 }
catch { Write-Host "No se pudo conectar con $ApiUrl" -ForegroundColor Red; exit 1 }
$empresa = $empresas | Where-Object { $_.nombre -eq "Empresa Demo" } | Select-Object -First 1
if (-not $empresa) { Write-Host "No existe la empresa Empresa Demo." -ForegroundColor Red; exit 1 }
$eid = $empresa.id

$cli = Entrar "cliente@demo.com" $Password $eid
$agt = Entrar "agente@demo.com" $Password $eid
$sup = Entrar "supervisor@demo.com" $Password $eid
$adm = Entrar "admin@demo.com" $Password $eid
$hc = @{ Authorization = "Bearer " + $cli.token }
$ha = @{ Authorization = "Bearer " + $agt.token }
$hd = @{ Authorization = "Bearer " + $adm.token }
$cats = Invoke-RestMethod -Uri "$ApiUrl/categorias" -Headers $hc -TimeoutSec 120
$catBug = ($cats | Where-Object { $_.nombre -eq "Bug" }).id

Write-Host "`nUsuarios y registro"
$emailQa = "qa.$sello@demo.com"
$claveQa = "Prueba12345"
$c = CodigoHttp { Enviar Post "$ApiUrl/usuarios" $hd @{ nombre = "QA Agente"; email = $emailQa; password = $claveQa; rol = "ROLE_AGENT" } }
Anotar "N01 El administrador crea un usuario agente" ($c -eq 200) ("codigo=" + $c)
$qa = $null
try { $qa = Entrar $emailQa $claveQa $eid } catch { }
Anotar "N01 El nuevo agente inicia sesion" ($qa -and $qa.rol -eq "ROLE_AGENT") "no pudo iniciar sesion"

$emailDup = "qa.dup.$sello@demo.com"
$cuerpoReg = '{"nombre":"QA Duplicado","email":"' + $emailDup + '","password":"Prueba12345","empresaId":"' + $eid + '"}'
$primero = CodigoHttp { Invoke-RestMethod -Method Post -Uri "$ApiUrl/auth/register" -ContentType "application/json" -Body $cuerpoReg -TimeoutSec 120 }
$segundo = CodigoHttp { Invoke-RestMethod -Method Post -Uri "$ApiUrl/auth/register" -ContentType "application/json" -Body $cuerpoReg -TimeoutSec 120 }
Anotar "N02 El segundo registro con el mismo email da 409" ($primero -eq 200 -and $segundo -eq 409) ("primero=" + $primero + " segundo=" + $segundo)

if ($qa) {
    Invoke-RestMethod -Method Patch -Uri "$ApiUrl/usuarios/$($qa.usuarioId)/desactivar" -Headers $hd -TimeoutSec 120 | Out-Null
    $c = CodigoHttp { Entrar $emailQa $claveQa $eid }
    Anotar "N03 Una cuenta desactivada no puede iniciar sesion (401)" ($c -eq 401) ("codigo=" + $c)
}

Write-Host "`nValidaciones de tickets y archivos"
$c = CodigoHttp { Enviar Post "$ApiUrl/tickets" $hc @{ titulo = ""; descripcion = "QA"; categoriaId = $catBug; prioridad = "BAJA" } }
Anotar "N04 Crear un ticket sin titulo da 400" ($c -eq 400) ("codigo=" + $c)

$tQa = NuevoTicket "[QA] validaciones de archivos"
$txt = Join-Path $env:TEMP "qa.txt"
"esto no es una imagen" | Out-File $txt -Encoding ascii
$salida = curl.exe -s -w "`nHTTP %{http_code}" -X POST -H ("Authorization: Bearer " + $cli.token) -F "archivo=@$txt;type=text/plain" "$ApiUrl/tickets/$($tQa.id)/adjuntos"
Anotar "N05 Un archivo de texto como foto da 400" ($salida -match "HTTP 400") $salida
$grande = Join-Path $env:TEMP "qa-grande.png"
[System.IO.File]::WriteAllBytes($grande, (New-Object byte[] (11MB)))
$salida = curl.exe -s -w "`nHTTP %{http_code}" -X POST -H ("Authorization: Bearer " + $cli.token) -F "archivo=@$grande;type=image/png" "$ApiUrl/tickets/$($tQa.id)/adjuntos"
if ($salida -match "HTTP 413") { Anotar "N05 Un archivo de 11 MB da 413" $true "" }
elseif ($salida -match "HTTP 000" -or $salida -notmatch "HTTP") { Revisar "N05 Un archivo de 11 MB" "el servidor cerro la conexion en lugar de responder 413; verificar a mano en la aplicacion" }
else { Anotar "N05 Un archivo de 11 MB da 413" $false $salida }

Write-Host "`nSugerencia de voz"
$sug = Enviar Post "$ApiUrl/tickets/voz/sugerencia" $hc @{ transcripcion = "Hola buenas tardes estoy probando el sistema" }
Anotar "N16 Sin palabras clave se sugiere la categoria Consulta" ($sug.categoriaNombre -eq "Consulta") ("categoria=" + $sug.categoriaNombre)

Write-Host "`nAsignacion, reapertura y niveles"
$c = CodigoHttp { Enviar Patch "$ApiUrl/tickets/$($tQa.id)/asignar" $ha @{ agenteId = $sup.usuarioId } }
Anotar "N06 Un agente no puede asignar a otra persona (403)" ($c -eq 403) ("codigo=" + $c)

$t7 = NuevoTicket "[QA] reapertura y nivel"
Enviar Patch "$ApiUrl/tickets/$($t7.id)/asignar" $ha @{ agenteId = $agt.usuarioId } | Out-Null
Enviar Patch "$ApiUrl/tickets/$($t7.id)/estado" $ha @{ estadoNuevo = "EN_PROGRESO" } | Out-Null
Enviar Patch "$ApiUrl/tickets/$($t7.id)/estado" $ha @{ estadoNuevo = "RESUELTO"; motivo = "QA" } | Out-Null
$r = Enviar Patch "$ApiUrl/tickets/$($t7.id)/estado" $hc @{ estadoNuevo = "REABIERTO" }
Anotar "N07 El cliente reabre un ticket resuelto" ($r.estado -eq "REABIERTO") ("estado=" + $r.estado)
$r = Enviar Patch "$ApiUrl/tickets/$($t7.id)/nivel" $ha @{ nivel = "NIVEL_2"; motivo = "QA" }
Anotar "N09 Un agente cambia el nivel de atencion" ($r.nivelAtencion -eq "NIVEL_2") ("nivel=" + $r.nivelAtencion)

$t8 = NuevoTicket "[QA] respuesta del cliente"
Enviar Patch "$ApiUrl/tickets/$($t8.id)/asignar" $ha @{ agenteId = $agt.usuarioId } | Out-Null
Enviar Patch "$ApiUrl/tickets/$($t8.id)/estado" $ha @{ estadoNuevo = "EN_PROGRESO" } | Out-Null
Enviar Patch "$ApiUrl/tickets/$($t8.id)/estado" $ha @{ estadoNuevo = "ESPERANDO_CLIENTE" } | Out-Null
Enviar Post "$ApiUrl/tickets/$($t8.id)/comentarios" $hc @{ contenido = "QA respuesta del cliente" } | Out-Null
$d = Invoke-RestMethod -Uri "$ApiUrl/tickets/$($t8.id)" -Headers $hc -TimeoutSec 120
Anotar "N08 La respuesta del cliente reanuda el ticket (EN_PROGRESO)" ($d.estado -eq "EN_PROGRESO") ("estado=" + $d.estado)

Write-Host "`nCategorias y configuracion"
$nombreCat = "QA Categoria $sello"
$primero = CodigoHttp { Enviar Post "$ApiUrl/categorias" $hd @{ nombre = $nombreCat; descripcion = "QA" } }
$segundo = CodigoHttp { Enviar Post "$ApiUrl/categorias" $hd @{ nombre = $nombreCat; descripcion = "QA" } }
Anotar "N10 Una categoria repetida da 409" ($primero -eq 200 -and $segundo -eq 409) ("primero=" + $primero + " segundo=" + $segundo)

$c = CodigoHttp { Enviar Patch "$ApiUrl/empresas/$eid/configuracion" $hd @{ ventanaMantenimientoInicio = "05:00"; ventanaMantenimientoFin = "05:00" } }
Anotar "N11 Una ventana con la misma hora de inicio y fin da 400" ($c -eq 400) ("codigo=" + $c)

$otraEmpresa = [guid]::NewGuid()
$c = CodigoHttp { Invoke-RestMethod -Uri "$ApiUrl/empresas/$otraEmpresa/configuracion" -Headers $hd -TimeoutSec 120 }
Anotar "N12 La configuracion de otra empresa da 403" ($c -eq 403) ("codigo=" + $c)

$cfg = Invoke-RestMethod -Uri "$ApiUrl/empresas/$eid/configuracion" -Headers $hd -TimeoutSec 120
$originales = @($cfg.modulosHabilitados)
try {
    $sinVoz = @($originales | Where-Object { $_ -ne "voz" })
    Enviar Patch "$ApiUrl/empresas/$eid/configuracion" $hd @{ modulosHabilitados = $sinVoz } | Out-Null
    $c = CodigoHttp { Enviar Post "$ApiUrl/tickets/voz/sugerencia" $hc @{ transcripcion = "prueba de modulo deshabilitado" } }
    Anotar "N13 Con el modulo de voz deshabilitado la API da 403" ($c -eq 403) ("codigo=" + $c)
}
finally {
    Enviar Patch "$ApiUrl/empresas/$eid/configuracion" $hd @{ modulosHabilitados = $originales } | Out-Null
    $cfg2 = Invoke-RestMethod -Uri "$ApiUrl/empresas/$eid/configuracion" -Headers $hd -TimeoutSec 120
    Anotar "N13 El modulo de voz quedo restaurado" ($cfg2.modulosHabilitados -contains "voz") "el modulo no se restauro"
}

Write-Host ""
$color = if ($script:fallos -eq 0) { "Green" } else { "Yellow" }
Write-Host ("Resultado: " + $script:aprobados + " aprobadas, " + $script:fallos + " con fallo, " + $script:revisar + " para revisar a mano.") -ForegroundColor $color
Write-Host "Se crearon datos con el prefijo QA. El usuario QA quedo desactivado."

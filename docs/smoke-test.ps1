# Verificacion automatica de TicketDesk (prueba de humo).
# Uso, desde PowerShell:
#   .\smoke-test.ps1 -ApiUrl "https://URL-DEL-BACKEND.onrender.com/api/v1"
# Para probar el backend local:
#   .\smoke-test.ps1 -ApiUrl "http://localhost:8080/api/v1"
#
# Requiere que existan los datos de prueba (empresa "Empresa Demo" y sus cuatro
# usuarios, contrasena Demo1234!). Crea algunos tickets de prueba en la base.

param([Parameter(Mandatory = $true)][string]$ApiUrl)

Add-Type -AssemblyName System.Drawing
$ApiUrl = $ApiUrl.TrimEnd('/')
$Password = "Demo1234!"
$script:aprobados = 0
$script:fallos = 0

function Anotar($nombre, $correcto, $detalle) {
    if ($correcto) { $script:aprobados++; Write-Host ("  [OK]    " + $nombre) -ForegroundColor Green }
    else { $script:fallos++; Write-Host ("  [FALLO] " + $nombre + " | " + $detalle) -ForegroundColor Red }
}

function Enviar($metodo, $url, $headers, $obj) {
    $json = $obj | ConvertTo-Json
    Invoke-RestMethod -Method $metodo -Uri $url -Headers $headers -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($json)) -TimeoutSec 120
}

function CodigoHttp($bloque) {
    try { & $bloque | Out-Null; return 200 } catch { return [int]$_.Exception.Response.StatusCode.value__ }
}

function Entrar($email, $empresaId) {
    $cuerpo = '{"email":"' + $email + '","password":"' + $Password + '","empresaId":"' + $empresaId + '"}'
    Invoke-RestMethod -Method Post -Uri "$ApiUrl/auth/login" -ContentType "application/json" -Body $cuerpo -TimeoutSec 120
}

Write-Host "Despertando el servidor (puede tardar hasta un par de minutos)..."
try { $empresas = Invoke-RestMethod -Uri "$ApiUrl/auth/empresas" -TimeoutSec 240 }
catch { Write-Host "No se pudo conectar con $ApiUrl" -ForegroundColor Red; exit 1 }

$empresa = $empresas | Where-Object { $_.nombre -eq "Empresa Demo" } | Select-Object -First 1
if (-not $empresa) { Write-Host "No existe la empresa Empresa Demo. Faltan los datos de prueba." -ForegroundColor Red; exit 1 }
$eid = $empresa.id

Write-Host "`nInicio de sesion de los cuatro roles"
$cli = Entrar "cliente@demo.com" $eid
$agt = Entrar "agente@demo.com" $eid
$sup = Entrar "supervisor@demo.com" $eid
$adm = Entrar "admin@demo.com" $eid
Anotar "Cliente inicia sesion" ($cli.token -and $cli.rol -eq "ROLE_USER") ("rol=" + $cli.rol)
Anotar "Agente inicia sesion" ($agt.token -and $agt.rol -eq "ROLE_AGENT") ("rol=" + $agt.rol)
Anotar "Supervisor inicia sesion" ($sup.token -and $sup.rol -eq "ROLE_SUPERVISOR") ("rol=" + $sup.rol)
Anotar "Administrador inicia sesion" ($adm.token -and $adm.rol -eq "ROLE_ADMIN") ("rol=" + $adm.rol)

$hc = @{ Authorization = "Bearer " + $cli.token }
$ha = @{ Authorization = "Bearer " + $agt.token }
$hs = @{ Authorization = "Bearer " + $sup.token }
$hd = @{ Authorization = "Bearer " + $adm.token }

Write-Host "`nCategorias y creacion de ticket"
$cats = Invoke-RestMethod -Uri "$ApiUrl/categorias" -Headers $hc -TimeoutSec 120
Anotar "Hay al menos 5 categorias" (@($cats).Count -ge 5) ("cantidad=" + @($cats).Count)
$catBug = ($cats | Where-Object { $_.nombre -eq "Bug" }).id
$nuevo = @{ titulo = "[smoke] ciclo completo"; descripcion = "Ticket de prueba automatica"; categoriaId = $catBug; prioridad = "MEDIA" }
$t1 = Enviar Post "$ApiUrl/tickets" $hc $nuevo
Anotar "El ticket nace en estado NUEVO, Nivel 1" ($t1.estado -eq "NUEVO" -and $t1.nivelAtencion -eq "NIVEL_1") ("estado=" + $t1.estado)

Write-Host "`nCiclo de vida y permisos"
$r = Enviar Patch "$ApiUrl/tickets/$($t1.id)/asignar" $ha @{ agenteId = $agt.usuarioId; motivo = "Tomo el ticket" }
Anotar "El agente toma el ticket (ASIGNADO)" ($r.estado -eq "ASIGNADO") ("estado=" + $r.estado)
$r = Enviar Patch "$ApiUrl/tickets/$($t1.id)/estado" $ha @{ estadoNuevo = "EN_PROGRESO" }
Anotar "Pasa a EN_PROGRESO" ($r.estado -eq "EN_PROGRESO") ("estado=" + $r.estado)
$r = Enviar Patch "$ApiUrl/tickets/$($t1.id)/estado" $ha @{ estadoNuevo = "RESUELTO"; motivo = "Resuelto en la prueba" }
Anotar "Pasa a RESUELTO" ($r.estado -eq "RESUELTO") ("estado=" + $r.estado)
$c = CodigoHttp { Enviar Patch "$ApiUrl/tickets/$($t1.id)/estado" $ha @{ estadoNuevo = "CERRADO" } }
Anotar "El agente no puede cerrar (409)" ($c -eq 409) ("codigo=" + $c)
$r = Enviar Patch "$ApiUrl/tickets/$($t1.id)/estado" $hc @{ estadoNuevo = "CERRADO" }
Anotar "El cliente confirma el cierre (CERRADO)" ($r.estado -eq "CERRADO") ("estado=" + $r.estado)
$c = CodigoHttp { Enviar Patch "$ApiUrl/tickets/$($t1.id)/nivel" $hc @{ nivel = "CRITICO" } }
Anotar "El cliente no puede cambiar el nivel (403)" ($c -eq 403) ("codigo=" + $c)
$c = CodigoHttp { Invoke-RestMethod -Uri "$ApiUrl/tickets/$($t1.id)/historial" -Headers $hc -TimeoutSec 120 }
Anotar "El cliente no puede ver el historial (403)" ($c -eq 403) ("codigo=" + $c)
$hist = Invoke-RestMethod -Uri "$ApiUrl/tickets/$($t1.id)/historial" -Headers $ha -TimeoutSec 120
Anotar "El historial registra los cambios (5 o mas)" (@($hist).Count -ge 5) ("cantidad=" + @($hist).Count)

Write-Host "`nAsignacion por supervisor"
$t2 = Enviar Post "$ApiUrl/tickets" $hc @{ titulo = "[smoke] asignacion por supervisor"; descripcion = "Prueba"; categoriaId = $catBug; prioridad = "BAJA" }
$r = Enviar Patch "$ApiUrl/tickets/$($t2.id)/asignar" $hs @{ agenteId = $agt.usuarioId; motivo = "Asignado por el supervisor" }
Anotar "El supervisor asigna el ticket a un agente" ($r.estado -eq "ASIGNADO" -and $r.agenteId -eq $agt.usuarioId) ("estado=" + $r.estado)

Write-Host "`nEscalado"
$t3 = Enviar Post "$ApiUrl/tickets" $hc @{ titulo = "[smoke] escalado"; descripcion = "Prueba"; categoriaId = $catBug; prioridad = "ALTA" }
Enviar Patch "$ApiUrl/tickets/$($t3.id)/asignar" $ha @{ agenteId = $agt.usuarioId } | Out-Null
Enviar Patch "$ApiUrl/tickets/$($t3.id)/estado" $ha @{ estadoNuevo = "EN_PROGRESO" } | Out-Null
$c = CodigoHttp { Enviar Patch "$ApiUrl/tickets/$($t3.id)/estado" $ha @{ estadoNuevo = "ESCALADO" } }
Anotar "Escalar sin motivo da error (400)" ($c -eq 400) ("codigo=" + $c)
$r = Enviar Patch "$ApiUrl/tickets/$($t3.id)/estado" $ha @{ estadoNuevo = "ESCALADO"; motivo = "Requiere revision tecnica" }
Anotar "Escalar con motivo: ESCALADO, Nivel 2" ($r.estado -eq "ESCALADO" -and $r.nivelAtencion -eq "NIVEL_2") ("estado=" + $r.estado + " nivel=" + $r.nivelAtencion)
Anotar "Se reasigna a otra persona (supervisor)" ($r.agenteId -and $r.agenteId -ne $agt.usuarioId) ("agente=" + $r.agenteNombre)

Write-Host "`nFotos (Cloudinary)"
$foto = Join-Path $env:TEMP "smoke.png"
$bmp = New-Object System.Drawing.Bitmap 40, 40
$bmp.Save($foto, [System.Drawing.Imaging.ImageFormat]::Png)
$salida = curl.exe -s -w "`nHTTP %{http_code}" -X POST -H ("Authorization: Bearer " + $agt.token) -F "archivo=@$foto;type=image/png" "$ApiUrl/tickets/$($t3.id)/adjuntos"
Anotar "El agente no puede subir fotos (403)" ($salida -match "HTTP 403") $salida
$salida = curl.exe -s -w "`nHTTP %{http_code}" -X POST -H ("Authorization: Bearer " + $cli.token) -F "archivo=@$foto;type=image/png" "$ApiUrl/tickets/$($t3.id)/adjuntos"
Anotar "El cliente sube una foto a Cloudinary (201)" ($salida -match "HTTP 201" -and $salida -match "cloudinary") $salida

Write-Host "`nCreacion por voz"
$texto = "Hola, tengo un error de pago, me cobraron dos veces la suscripcion"
$sug = Enviar Post "$ApiUrl/tickets/voz/sugerencia" $hc @{ transcripcion = $texto }
Anotar "La sugerencia propone la categoria Facturacion" ($sug.categoriaNombre -like "Facturaci*") ("categoria=" + $sug.categoriaNombre)
$wav = Join-Path $env:TEMP "smoke.wav"
$datos = New-Object byte[] 8000
for ($i = 0; $i -lt $datos.Length; $i++) { $datos[$i] = 128 }
$ms = New-Object System.IO.MemoryStream
$bw = New-Object System.IO.BinaryWriter($ms)
$bw.Write([System.Text.Encoding]::ASCII.GetBytes("RIFF")); $bw.Write([int](36 + $datos.Length))
$bw.Write([System.Text.Encoding]::ASCII.GetBytes("WAVEfmt ")); $bw.Write([int]16); $bw.Write([int16]1); $bw.Write([int16]1)
$bw.Write([int]8000); $bw.Write([int]8000); $bw.Write([int16]1); $bw.Write([int16]8)
$bw.Write([System.Text.Encoding]::ASCII.GetBytes("data")); $bw.Write([int]$datos.Length); $bw.Write($datos)
$bw.Flush()
[System.IO.File]::WriteAllBytes($wav, $ms.ToArray())
$salida = curl.exe -s -w "`nHTTP %{http_code}" -X POST -H ("Authorization: Bearer " + $cli.token) -F "audio=@$wav;type=audio/wav" -F "transcripcion=$texto" -F "categoriaId=$($sug.categoriaId)" -F "prioridad=ALTA" "$ApiUrl/tickets/voz"
Anotar "Se crea el ticket por voz con audio (201)" ($salida -match "HTTP 201" -and $salida -match '"canalOrigen":"VOZ"') $salida

Write-Host "`nAdministracion"
$cfg = Invoke-RestMethod -Uri "$ApiUrl/empresas/$eid/configuracion" -Headers $hd -TimeoutSec 120
Anotar "El administrador lee la configuracion" ($cfg.modulosHabilitados.Count -ge 1) "sin modulos"
$us = Invoke-RestMethod -Uri "$ApiUrl/usuarios" -Headers $hd -TimeoutSec 120
Anotar "El administrador lista los usuarios (4 o mas)" (@($us).Count -ge 4) ("cantidad=" + @($us).Count)
$c = CodigoHttp { Invoke-RestMethod -Uri "$ApiUrl/usuarios" -Headers $hc -TimeoutSec 120 }
Anotar "El cliente no puede listar usuarios (403)" ($c -eq 403) ("codigo=" + $c)

Write-Host ""
$color = if ($script:fallos -eq 0) { "Green" } else { "Yellow" }
Write-Host ("Resultado: " + $script:aprobados + " aprobadas, " + $script:fallos + " con fallo.") -ForegroundColor $color

"""Remote UI checks on an explicitly selected TV. Temporarily changes/restores volume and brightness.

Run only with device-test authorization. The package defaults to the separate preview app.
"""
import argparse
import json
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('--serial', required=True)
parser.add_argument('--model', required=True)
parser.add_argument('--output', required=True)
args = parser.parse_args()
output = Path(args.output)
output.mkdir(parents=True, exist_ok=True)
package = 'com.thewhitewolf1985.tvhome.preview'
checks = []

def adb(*args):
    return subprocess.check_output(['adb', '-s', globals()['args'].serial, *args], timeout=35).decode('utf-8').strip()

def key(code):
    adb('shell', 'input', 'keyevent', str(code))

def tree():
    # Vendor volume overlays sometimes make UIAutomator return a null root. Never read an old dump.
    for attempt in range(3):
        name = '/data/local/tmp/tv-header-test-' + str(time.time_ns()) + '.xml'
        result = adb('shell', 'uiautomator', 'dump', name)
        if 'dumped to:' in result:
            value = adb('shell', 'cat', name)
            adb('shell', 'rm', name)
            return ET.fromstring(value)
    raise AssertionError('UIAutomator could not capture a fresh root')

def require(condition, message):
    if not condition:
        raise AssertionError(message)
    checks.append(message)

def text_exists(root, text):
    return any(text in n.get('text', '') for n in root.iter('node'))

def focused(root):
    return next(n for n in root.iter('node') if n.get('focused') == 'true')

def capture(name):
    path = output / name
    # MediaTek's screencap prints vendor diagnostics to stdout, even with exec-out.
    remote = '/data/local/tmp/tv-header-' + name
    adb('shell', 'screencap', '-p', remote)
    adb('pull', remote, str(path))
    adb('shell', 'rm', remote)

require(adb('shell', 'getprop', 'ro.product.model') == args.model, 'Device model matches')
original_brightness = adb('shell', 'settings', 'get', 'system', 'screen_brightness')
original_home = adb('shell', 'cmd', 'package', 'resolve-activity', '--brief', '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME')
original_disabled = adb('shell', 'pm', 'list', 'packages', '-d')
adb('shell', 'am', 'force-stop', package)
adb('shell', 'am', 'start', '-n', package + '/com.ansub.tvhome.MainActivity')
root = tree()
require(focused(root).get('package') == package, 'Preview is foreground')
key(19)  # top app -> header
root = tree()
require('impostazioni rapide' in focused(root).get('content-desc', '').lower(), 'Up selects the header')
header_bounds = focused(root).get('bounds')
capture('header-focused.png')
key(20)
root = tree()
require(not text_exists(root, 'Impostazioni rapide'), 'Down from the header returns to tiles without opening the shade')
require(focused(root).get('bounds') != header_bounds, 'Tile navigation remains available')
key(19)
key(23)
root = tree()
require(text_exists(root, 'Impostazioni rapide'), 'OK on the selected header opens the shade')
require(focused(root).get('class') == 'android.widget.SeekBar', 'Volume slider has initial focus')
volume_before = next(n.get('text') for n in root.iter('node') if n.get('text', '').startswith('Volume ·'))
key(22)
root = tree()
require(not text_exists(root, volume_before), 'Volume increases through the app')
key(21)
root = tree()
require(text_exists(root, volume_before), 'Volume restored through the app')
key(20)
root = tree()
require('Luminosità' in focused(root).get('content-desc', ''), 'Down selects brightness')
key(22)
changed = adb('shell', 'settings', 'get', 'system', 'screen_brightness')
try:
    require(changed != original_brightness, 'Brightness setting changes through the app')
    key(21)
    require(adb('shell', 'settings', 'get', 'system', 'screen_brightness') == original_brightness,
            'Brightness restored through the app')
finally:
    if adb('shell', 'settings', 'get', 'system', 'screen_brightness') != original_brightness:
        adb('shell', 'settings', 'put', 'system', 'screen_brightness', original_brightness)
key(20)
root = tree()
require(focused(root).get('text') == 'Muto', 'Down selects mute')
key(23)
root = tree()
require(text_exists(root, 'Volume · Muto') and text_exists(root, 'Attiva audio'), 'Mute state is shown')
key(23)
root = tree()
require(text_exists(root, volume_before), 'Mute toggled back, previous volume preserved')
key(4)
root = tree()
require(focused(root).get('bounds') == header_bounds, 'Back closes the shade and returns focus to the header')
key(4)
root = tree()
require(focused(root).get('content-desc', '') != 'Impostazioni rapide', 'Back returns to the app grid')
key(20)  # focus lower row, scrolling as necessary
root = tree()
require(any(n.get('bounds') == header_bounds and 'impostazioni rapide' in n.get('content-desc', '').lower()
            for n in root.iter('node')), 'Header remains fixed when the lower row is selected')
capture('header-lower-row.png')
require(adb('shell', 'cmd', 'package', 'resolve-activity', '--brief', '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME') == original_home,
        'Default Home preserved')
require(adb('shell', 'pm', 'list', 'packages', '-d') == original_disabled, 'Disabled packages preserved')
key(19)
key(19)
key(23)
root = tree()
require(text_exists(root, 'Impostazioni rapide'), 'Shade left open for user testing')
capture('header-open.png')
(output / 'runtime-checks.json').write_text(json.dumps({'checks': checks, 'brightness_before': original_brightness,
    'brightness_after': adb('shell', 'settings', 'get', 'system', 'screen_brightness'),
    'note': 'Readback verifies Android values; perceived audio and physical panel brightness need user confirmation.'}, indent=2), encoding='utf-8')
print(json.dumps({'passed': len(checks), 'output': str(output)}, indent=2))

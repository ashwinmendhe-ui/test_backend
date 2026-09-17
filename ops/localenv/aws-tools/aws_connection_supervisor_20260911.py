"""Run as uid 1000; never write credentials to stdout or Docker logs."""
import os, json, time, subprocess, threading, socket, socketserver, select, signal
from pathlib import Path
from datetime import datetime, timezone
ROOT = Path('/home/dhiveserver/robopilot-local-20260911')
OUT = ROOT/'aws-tools/auto-runtime'
AWS = str(ROOT/'aws-tools/bin/aws')
os.umask(0o077)
state = {'credentials': 'starting', 'tunnel': 'starting'}
lock = threading.Lock()
def status(**values):
    with lock:
        state.update(values)
        state['checked_at'] = datetime.now(timezone.utc).isoformat()
        p = OUT/'status.tmp'
        p.write_text(json.dumps(state))
        p.replace(OUT/'status.json')
def credentials():
    while True:
        try:
            p = subprocess.run([AWS,'configure','export-credentials','--profile','robopilot-server','--format','process'],stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,timeout=45)
            if p.returncode: raise RuntimeError()
            data = json.loads(p.stdout)
            assert data['Version']==1 and data['Expiration'] and data['SessionToken']
            temp = OUT/'credentials.tmp'
            temp.write_text(json.dumps(data)); temp.replace(OUT/'credentials.json')
            status(credentials='ok', expiration=data['Expiration'])
        except Exception:
            status(credentials='refresh_failed_check_certificate_or_network')
        time.sleep(60)
class Handler(socketserver.BaseRequestHandler):
    def handle(self):
        try:
            with socket.create_connection(('127.0.0.1',17879),timeout=10) as peer:
                while True:
                    ready,_,_=select.select([self.request,peer],[],[],60)
                    if not ready: return
                    for src in ready:
                        data=src.recv(65536)
                        if not data:return
                        (peer if src is self.request else self.request).sendall(data)
        except OSError: pass
class Server(socketserver.ThreadingTCPServer):
    allow_reuse_address=True
    daemon_threads=True
def bridge():
    while True:
        try:
            with Server(('172.18.0.1',17880),Handler) as s: s.serve_forever()
        except OSError: time.sleep(5)
threading.Thread(target=credentials,daemon=True).start()
threading.Thread(target=bridge,daemon=True).start()
args=[AWS,'--profile','robopilot-server','ssm','start-session','--target','i-0a4cf711bd8b8fa73','--document-name','ROBOPILOT-Local100-AI7879','--parameters',json.dumps({'portNumber':['7879'],'localPortNumber':['17879']})]
while True:
    p=subprocess.Popen(args,stdin=subprocess.DEVNULL,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,start_new_session=True)
    status(tunnel='connecting',tunnel_pid=p.pid)
    while p.poll() is None:
        try:
            with socket.create_connection(('127.0.0.1',17879),timeout=2): pass
            status(tunnel='listening',tunnel_pid=p.pid)
        except OSError: status(tunnel='connecting',tunnel_pid=p.pid)
        time.sleep(5)
    status(tunnel='reconnecting',tunnel_pid=None)
    try: os.killpg(p.pid,signal.SIGTERM)
    except ProcessLookupError: pass
    time.sleep(15)

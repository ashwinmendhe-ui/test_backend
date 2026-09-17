"""Offline checks only. Does not start services or print configuration values."""
from pathlib import Path
import ast, hashlib, json, re
r=Path(__file__).resolve().parent
m=json.loads((r/'MANIFEST.json').read_text(encoding='utf-8'))
for name,digest in m['sha256'].items():
 p=r/name
 assert hashlib.sha256(p.read_bytes()).hexdigest()==digest, 'Hash mismatch: '+name
 s=p.read_text(encoding='utf-8')
 assert not re.search(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY|\b(?:AKIA|ASIA)[A-Z0-9]{16}\b',s), 'Secret pattern: '+name
 if p.suffix=='.py':ast.parse(s)
 if p.suffix=='.json':json.loads(s)
sql=(r/'database/schema.sql').read_text(encoding='utf-8')
assert len(re.findall(r'CREATE TABLE public\.',sql))==14
assert not re.search(r'(?m)^(INSERT INTO|COPY .* FROM stdin)',sql)
print('Package hashes, syntax, secret patterns and schema-only checks passed.')

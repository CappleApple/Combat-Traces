"""Start, reload, and cleanly stop the isolated development server through loopback RCON."""
import os, pathlib, secrets, socket, struct, subprocess, time
ROOT=pathlib.Path(__file__).resolve().parents[1]
RUN=ROOT/"run-server"
RUN.mkdir(exist_ok=True)
password=secrets.token_hex(16)
(RUN/"eula.txt").write_text("eula=true\n")
(RUN/"server.properties").write_text("\n".join([
 "server-ip=127.0.0.1","server-port=25598","online-mode=false","enable-rcon=true",
 "rcon.port=25599","rcon.password="+password,"level-name=smoke-world",
 "level-type=minecraft:flat","generate-structures=false","spawn-protection=0",
 "view-distance=3","simulation-distance=3","max-tick-time=60000","sync-chunk-writes=false"
])+"\n")
def read_exact(sock,n):
 data=b""
 while len(data)<n:
  part=sock.recv(n-len(data))
  if not part: raise EOFError("RCON closed")
  data+=part
 return data
def packet(sock,request,kind,text):
 payload=struct.pack("<ii",request,kind)+text.encode()+b"\0\0"
 sock.sendall(struct.pack("<i",len(payload))+payload)
 size=struct.unpack("<i",read_exact(sock,4))[0]
 data=read_exact(sock,size)
 ident,type_=struct.unpack("<ii",data[:8])
 return ident,data[8:-2].decode(errors="replace")
def command(text):
 with socket.create_connection(("127.0.0.1",25599),timeout=5) as sock:
  ident,_=packet(sock,1,3,password)
  if ident==-1: raise RuntimeError("RCON authentication failed")
  return packet(sock,2,2,text)[1]
env=os.environ.copy()
for phase,without in [("fresh-without-bettercombat",True),("restart-with-bettercombat",False)]:
 log=ROOT/".work"/("server-"+phase+".log")
 args=[str(ROOT/"gradlew.bat"),"runServer","--console=plain"]
 if without: args+=["-PwithoutBetterCombat"]
 with log.open("w",encoding="utf-8") as out:
  process=subprocess.Popen(args,cwd=ROOT,stdout=out,stderr=subprocess.STDOUT,env=env)
  try:
   deadline=time.monotonic()+150
   while time.monotonic()<deadline:
    text=log.read_text(encoding="utf-8",errors="replace")
    if "Done (" in text: break
    if process.poll() is not None: raise RuntimeError(phase+" exited before ready; inspect "+str(log))
    time.sleep(.25)
   else: raise TimeoutError(phase+" startup timed out")
   print("PASS "+phase+": dedicated server reached Done",flush=True)
   print(command("list"),flush=True)
   print(command("reload"),flush=True)
   deadline=time.monotonic()+30
   while time.monotonic()<deadline:
    text=log.read_text(encoding="utf-8",errors="replace")
    if text.count("Loaded 1290 recipes")>=2 or "Reloading!" in text:
     time.sleep(1);break
    time.sleep(.25)
   command("save-all flush")
   try: command("stop")
   except (EOFError,ConnectionError): pass
   process.wait(timeout=45)
   if process.returncode!=0: raise RuntimeError("Server failed to shut down cleanly")
   text=log.read_text(encoding="utf-8",errors="replace")
   if "Failed to load class" in text or "invalid dist" in text: raise RuntimeError("Distribution safety failure")
   print("PASS "+phase+": reload, save, and clean shutdown",flush=True)
  finally:
   if process.poll() is None:
    try: command("stop");process.wait(timeout=30)
    except Exception: process.terminate()
print("COMBAT TRACES DEDICATED SERVER SMOKE PASS",flush=True)


let len = 560*448;
let pcnt = (system.maxmem() / len)|0;
if (pcnt < 2) {
  printerrln("Not enought RAM on the system to test!");
  return;
}
let c = 0;
print("Rendering plane ");
for (c = 0; c < pcnt; c++) {
  print(`${c} `);
  for (let i = 0; i < len; i++) {
    sys.poke(c*len + i, c * ((256/pcnt)|0) + 1);
  }
}
println("Let's see how fast DMA really is!");
c = 0;
while (1) {
  dma.ramToFrame(len * c, 0, len);
  c = (c + 1) % pcnt;
}
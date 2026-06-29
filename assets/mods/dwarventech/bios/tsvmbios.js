con.reset_graphics();con.curs_set(0);con.clear();
graphics.resetPalette();graphics.setBackground(0,0,0);

let logo = gzip.decomp(base64.atob("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"));
// display logo in kickin' ass-style of panasonic
// hide entire framebuffer with black text to hide the slow image drawing
/*
con.color_pair(0,0);
for(let i=0;i<2560;i++)graphics.putSymbolAt(1+(i/80)|0,1+(i%80),239);
// draw logo
for(let i=0;i<logo.length;i++){graphics.plotPixel(i%560,95+(i/560)|0,logo[i])}
// scramble lines
let m=5;let r=()=>{let i=Math.random()*2-1;return(i<0)?i-1:i+1};
let o=[];for(let y=0;y<164;y++){
let k=Math.round(r()*560/m)|0;
o.push(k);graphics.setLineOffset(95+y,k*m);}
// unhide screen
graphics.setBackground(0,68,255);con.color_pair(239,255);
for(let i=0;i<2560;i++)graphics.putSymbolAt(1+(i/80)|0,1+(i%80),0);
// unscramble
let tmr=0;let n=560*2;while(n>0){
for(let y=0;y<164;y++){o[y]-=Math.sign(o[y]);graphics.setLineOffset(95+y,o[y]*m);}
// wait for timer
tmr=sys.nanoTime();while(sys.nanoTime()-tmr<300000*m)Math.sqrt(tmr) // waste some cpu time
n-=m;}
*/



// display logo in mundane, true-to-msx way
graphics.setFramebufferScroll(0,-164);
// hide entire framebuffer with black text to hide the slow image drawing
con.color_pair(0,0);
for(let i=0;i<2560;i++)graphics.putSymbolAt(1+(i/80)|0,1+(i%80),239);
// draw logo
for(let i=0;i<logo.length;i++){graphics.plotPixel(i%560,95+(i/560)|0,logo[i])}
// cover up bottom part with text characters (!)
graphics.setBackground(0,68,255);con.color_pair(14,255);
for(let y=1;y<19;y++)for(let x=1;x<81;x++)graphics.putSymbolAt(y,x,32);
for(let x=1;x<81;x++)graphics.putSymbolAt(19,x,220);
for(let y=20;y<33;y++)for(let x=1;x<81;x++)graphics.putSymbolAt(y,x,219);
// scroll up
let tmr=sys.nanoTime();
let tlen=1500000000;
while(1){let tdiff=sys.nanoTime()-tmr;if(tdiff>=tlen)break;
graphics.setFramebufferScroll(0,-((1.0-tdiff/tlen)*164)|0);}


// show how much ram is there
con.color_pair(239,14);
let vramstr=`VIDEO RAM : ${256 * sys.peek(-131084)} Kbytes`;
let uramstr=` USER RAM : ${sys.maxmem()>>>10} Kbytes`;
con.move(20,(80-vramstr.length)/2);println(vramstr);
con.move(21,(80-uramstr.length)/2);println(uramstr);

///////////////////////////////////////////////////////////////////////////////


// probe bootable device

/*var _BIOS = {};

// Syntax: [Port, Drive-number]
// Port #0-3: Serial port 1-4
//	  #4+ : Left for future extension
// Drive-number always starts at 1
_BIOS.FIRST_BOOTABLE_PORT = [0,1]; // ah screw it

Object.freeze(_BIOS);*/

///////////////////////////////////////////////////////////////////////////////

// make user wait around because why not

tmr = sys.nanoTime();
while (sys.nanoTime() - tmr < 2147483648) sys.spin();
// clear screen
graphics.clearPixels(255);con.color_pair(239,255);
con.clear();con.move(1,1);

///////////////////////////////////////////////////////////////////////////////

// load a bootsector using 'LOADBOOT'
let portNumber = 0;
let driveStatus = 0;
while (portNumber < 4) {
	if (com.areYouThere(portNumber)) {
		com.sendMessage(portNumber, "LOADBOOT");
		driveStatus = com.getStatusCode(portNumber);
		if (driveStatus == 0) break;
	}
	portNumber += 1;
}
if (portNumber < 4) {
//	eval(com.fetchResponse(portNumber).trimNull());
    // using Function() so that BIOS variables won't get leaked in
	{Function("\"use strict\";var _BIOS={};_BIOS.FIRST_BOOTABLE_PORT=[0,1];Object.freeze(_BIOS);"+com.fetchResponse(portNumber).trimNull())()};
}
else {
	printerrln("No bootable medium found.");
}

sys.poke(-90,128)
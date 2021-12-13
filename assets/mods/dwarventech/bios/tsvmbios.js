while (1) {
con.reset_graphics();con.curs_set(0);con.clear();
graphics.resetPalette();graphics.setBackground(0,0,0);

let logo = gzip.decomp(base64.atob("H4sICJoBTGECA3Rzdm1sb2dvLnJhdwDtneu2nCoQhPf7v6xLEMUL5lxyVk6yhxm7mmZGpfqnK7uC+gkN1TA/fhTFF+Ni8eOjwedPXsgLeSEvDPLCIC8M8sIgL+SFvJAX8kJeGOSFQV4Y5IVBXsgLeSEv5IW8MMgLow1e1i4XfH/kJR8deSEvcl48eSEvAC+RvJAXgJedvJAXOS9DR17Ii5yXSF7IC8DLTl7Ii5yX0JEX8iLnZSUv5EXOy7Nsl7yQF6h7IS/kBcheyAt5eYx+Jy/kRc7L0pEX8iLmZezIC3kR8zJ05IW8iHnxO3khL2JeDnAhL+Tlj8HoABfyQl6kqS55IS9/rrssHXkhL1Jewt6RF/Ii5GVYO4vYctouxGVLe2cXXvHg3TeN3eeu6rR9lRafl5ewGr3I6RHEOXXmMSse/PeSwTV7Vac9V2nxSXkZotmnv/ffvulYAZZ//h8HP/f+e0tC9qpK2+01WnxSXtZq372bu1oxwc/9u+mesld12lOVFp+Ul65SXtHHrl5s8HNfs+9vNdHeqrT4/rz8/kxC6mrGUJiR/hwfvIn2UKXFDfAyIhlgWSyFGenyopWo9lKlxffn5f9s122VcUHzx4casCF7VaXt9hotboCX+OsJpq56ROipj9mRczTRjlVa3AAvTmhym0QqykjHl3kqpp2qtPj+vKxY/1waoSAj/TlyDibaoUqLG+AlvG8w+h1PTUY6H+SpiPZapcX35yX18sWIN5tIDz2eP+oH5dq+Sosb4GV6z0RaY8lM2Q99MtGeq7S4AV4cOJqbm1XyjDQc5qli7X6v0uL787J8PfHv6sVobh3h2mOVFjfAi4fWIt5qIq3ZhZDVRHur0uL787J95auPTmAiPSwHOckikUx7qNLiBngZ35zsApZMzP5VNNFeqrT4/rz8zOTe3L3ILBnIOgK14aVJ3ES6Jy/z+7OX3+bwmHXUy/JUifZUpcUN8OIhJ+WtJhJmHWHaqUqL78/Lqkr+3mIi+ezI6U20Q5UWN8BL+ES2K7Nk5uzIOZtor1VafH9e/rOO0vt56RyakXp5nnqoXaXFDfAyfWLx5fe1N3lGugF5agQn6jYtboCXt1tHj664NCMdgZ7wQFvpfaS+dV6Wr8/MpgWWzJB9WYOJ9lilxQ3wMujWOt9hIi3ZwWAx0d6qtPj+vGyFz89k6UeY7TpsVdYbFUrJVS+wfxrBp2DxalIUf0gwXMytI5n2Ujp+t87LbrsQLk0TXlkye3adSG76vNAuqGqHTKT78vL6L3stL4cvZpIXSvXoPG4ytI503w55QeNoLTaJh7IJzrOSoXWkM5E4HqFxmFgO5tbRsXaZVzaQl2r57rFNswo7pkXhcq2G1pHKRLovL2Xz6T1tSwxOZQM7WaGUhwv6n2qXeh+OvNis16V5wBfeo6xQSrUqGw2tI42JdF9erPyAFB2onLdkZIVSq0b7kOBN1eK2eDH0G2eH9f5BkJHm99jvXqN9eKuDRrUxXkzrGWKPDHWr2jqKKu2jTmlRqTbGi229VArI7NVrC6W8Rlsww1eoNseLcT3mDKA4H2ZT69OruLZkBRFXbY4X63rvzYlX3x93ssv22AeNdi9xKPAWN8eLeQFvcmoTSWYd/XsV1j5EwZXZXs3wYl5ht3vpELAdZKTTi6uo9iYaalDVBnmxr/j+Zf2DJpLPLqjmr6LawlRWbXu1w0uFHUi/hiSsbEpWKLWotBdhx1FS6NUILxW2lGzS6mr3KiMdnl9FtQ/vcdSotslLjT0CMzApwayjDZrwwFO13iTjvTcvNc4jC7iJJLOORo1BBZifOturKV5qbFr777ECRo/QOurlC7ZBfoNeo9osLzU23Ue0bEp2PPOsKslCire0hV4t8VJjG5LDvmyxdfSF9xpQnwH0Re3yUuE8+BkzkWTHM6/Q0vSsKj43MJFuz0uN35tw0MxEbh3Bsx5wzmNgIt2flwq/ZxNlII7ZbDe/x/7b5ESoDW6eE6o2zov9kJSQlVXZ8cwRrD7eVGu20rXgtnmx/z2+QebcDLn1V/f19CriCg3SfwSrkpdatVOSzxuzjuTzukXVXRSbSI3wYvx7wklmyfydPz6svw7ZVdnhcPtJThtPRwSq5OXnVMLUS3LS6cmYJW18Oe2VaiumO8UmUjO8/J0zGA5KQbj80cv22E+KITT1muWUY1Xy8j8x0WpUisLl1Sk7wfWvp71C7cMO02tUA3n5Y4YwmyCzCC2ZlP3kZ9G66pH20dCymp4W0Cgv//QyIS5bKlvE25T+t3++897cWw86VUde8OgnoS+TFJhNwlWysp4wKVUjedHEa2B2XQXfUaGUZXVgVKq+znjJy7MeRvY/O/wHWQfpmkeRU/r0FMMyE+navPQf5wU6ZubZHvtnUXKEzaJWXa/MS61T6KzGI2jXrc9aR77Kjt5Br+ovzEu1U+iM8l2kgO/5Hnv74sCtQHW+MC8fOtUdeB3yk29D1joK6k5O2/OWlE2dnZflnLwsgCXzZ58UhNNeTBvyDUtMpLPzEs/JS1TUSrzaY29dhzEXqW7X5SWck5eAWDKwdQRrQylr0d77s/PizsmLw3Os/PHMS5X8bStUXS7Ly0d+tRNca5edoft6j/2z0P1q2lio+rzXOz0v8xl5mfGs9GCPvWnGe1gld6gaL8vLcEZeBjwpx6yjsoQ/Fqumy/JyxgEp4UkWaB2VJXCuXDVclpcTzqgjWoQk2WP/LPCfHlkNVNfL8nLCGZLDZ/2odVSyohAMVHd/VV7Ol/E+9gqHpdcpuxAvOoUdPvNIdO5Pr9x7fwFe3Om7F6ElA1lHehNpMlF9klpdgJezZTBRw/SIWkf678XZqI6X5aU/1RQp391LtqauAvDKPdfFSHW7LC/nMpGC1pIBrSOtieStVIfL8nKmlHdWWzJR2RFgJtJmprpcl5fzlE1takvGJ8n3W2wijWaq2f7vIry4k6QwyaktmUXdESAm0t7bqU7X5aXGKXQaI8/ZjZnyjgDRng1V04V5qXAKnQIXb1fatCOV6nJtb6kaLszLCYak5AyNHqQjkGuvpqrrlXmxP4UOTXWd5azfQ/cu1Q6mqpnh90K8fHhafdghQMuKG3bnQu3U26rGa/NifAodNBYJvlzE6Angncu0J2PVxyTrWrwYn0IHeEaSDxcwenZ0X6ZM21mrjhfnxfYUOvFQJHwPcqMnwvct0V7MVbfL82J5Cp1sJIrir1Zca7w7+K4l2oO9qr8+L19mp9AJYJmhdyCdwa2Kez7W3iqozrfg5cvmFLpXPUDalhjQbkBq9ATFDR9rjxVUv/eEl+WF8ZEgLwzywiAvDPLC509eyAt5IS8M8sIgLwzywiAv5IW8kBfyQl4Y5IVBXhjkhUFeyAt5IS/khbwwyAuDvDDIC+OWvPwFgd7gz8BmAQA="));



// display logo in mundane, true-to-msx way
graphics.setFramebufferScroll(0,-164);
// hide entire framebuffer with black text to hide the slow image drawing
con.color_pair(0,0);
for(let i=0;i<2560;i++)graphics.putSymbolAt(1+(i/80)|0,1+(i%80),239);
// draw logo
for(let i=0;i<logo.length;i++){graphics.plotPixel(i%560,95+(i/560)|0,logo[i])}
// cover up bottom part with text characters (!)
graphics.setBackground(0,4,15);con.color_pair(14,255);
for(let y=1;y<19;y++)for(let x=1;x<81;x++)graphics.putSymbolAt(y,x,32);
for(let x=1;x<81;x++)graphics.putSymbolAt(19,x,220);
for(let y=20;y<33;y++)for(let x=1;x<81;x++)graphics.putSymbolAt(y,x,219);
// scroll up
let tmr=sys.nanoTime();
let tlen=1073741824;
while(1){let tdiff=sys.nanoTime()-tmr;if(tdiff>=tlen)break;
graphics.setFramebufferScroll(0,-((1.0-tdiff/tlen)*164)|0);}


// show how much ram is there
con.color_pair(239,14);
let vramstr="VIDEO RAM : 256 Kbytes";
let uramstr=` USER RAM : ${system.maxmem()>>>10} Kbytes`;
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
    break;
}
else {
	printerrln("No bootable medium found.");
}

}
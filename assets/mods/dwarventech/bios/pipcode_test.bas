10 print("Polling...")
50 for i=0 to 160*140-1
63 poke(-1048576-i,int(rnd(1)*16))
70 next
80 goto 10
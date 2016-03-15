package com.Torvald.Terrarum.MapGenerator;

import com.Torvald.Rand.HQRNG;

import java.util.Random;

public class FloatingIslandsPreset {
	
	public static final int PRESETS = 5;
	
	static int[][] generatePreset(HQRNG random) {
		int index = random.nextInt(PRESETS);
		return generatePreset(index, random);
	}

	static int[][] generatePreset(int index, Random random){
		if (index == 0){			
			return processPreset(random, FloatingIslePreset01.data, FloatingIslePreset01.w, FloatingIslePreset01.h);
		}
		else if (index == 1){
			return processPreset(random, FloatingIslePreset02.data, FloatingIslePreset02.w, FloatingIslePreset02.h);
		}
		else if (index == 2){
			return processPreset(random, FloatingIslePreset03.data, FloatingIslePreset03.w, FloatingIslePreset03.h);
		}
		else if (index == 3){
			return processPreset(random, FloatingIslePreset04.data, FloatingIslePreset04.w, FloatingIslePreset04.h);
		}
		else if (index == 4){
			return processPreset(random, FloatingIslePreset05.data, FloatingIslePreset05.w, FloatingIslePreset05.h);
		}
		return null;
	}

	private static int[][] processPreset(Random random, int[] preset, int w, int h){
		int[][] temp = new int[h][w];
		int counter = 0;
		boolean mirrored = random.nextBoolean();
		
		for (int i = 0; i < h; i++){
			for (int j = 0; j < w; j++){				
				if (!mirrored){
					if (counter < preset.length - 1){
						temp[i][j] = preset[counter];
						counter++;
					}
					else{
						temp[i][j] = 0;
					}
				}
				else{
					if (counter < preset.length - 1){
						temp[i][w - 1 - j] = preset[counter];
						counter++;
					}
					else{
						temp[i][w - 1 - j] = 0;
					}
				}
			}
		}
				
		return temp;
	}
}

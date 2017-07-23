#version 120


varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;


uniform vec3 topColor;
uniform vec3 bottomColor;
uniform float parallax = 0.0; // +1.0: all top col, -1.0: all bototm col, 0.0: normal grad
uniform float parallax_size = 1.0/3.0; // 0: no parallax


// "steps" of R, G and B. Must be integer && equal or greater than 2
uniform float rcount = 64.0; // it even works on 256.0!
uniform float gcount = 64.0; // using 64: has less banding and most monitors are internally 6-bit
uniform float bcount = 64.0;


/*int bayer[7 * 7] = int[](
32,42,10,27,37,5,15,
1,18,28,45,13,23,40,
26,36,4,14,31,48,9,
44,12,22,39,0,17,34,
20,30,47,8,25,35,3,
38,6,16,33,43,11,21,
7,24,41,2,19,29,46
); // I kind of accidentally create it...
float bayerSize = 7.0;*/

int bayer[9 * 9] = int[](
50,71,2,23,44,56,77,17,29,
72,12,33,45,66,6,18,39,60,
22,43,55,76,16,28,49,70,1,
53,65,5,26,38,59,80,11,32,
75,15,27,48,69,0,21,42,54,
25,37,58,79,10,31,52,64,4,
47,68,8,20,41,62,74,14,35,
78,9,30,51,63,3,24,36,57,
19,40,61,73,13,34,46,67,7
); // I kind of accidentally create it...
float bayerSize = 9.0;



float bayerDivider = bayerSize * bayerSize;


vec4 nearestColour(vec4 incolor) {
    vec4 rgbaCounts = vec4(rcount, gcount, bcount, 1.0);


    vec4 color = incolor;

    color.r = floor((rgbaCounts.r - 1.0) * color.r + 0.5) / (rgbaCounts.r - 1.0);
    color.g = floor((rgbaCounts.g - 1.0) * color.g + 0.5) / (rgbaCounts.g - 1.0);
    color.b = floor((rgbaCounts.b - 1.0) * color.b + 0.5) / (rgbaCounts.b - 1.0);
    color.a = 1.0;

    return color;
}

void main(void) {
    float spread = 1.0 / (0.299 * (rcount - 1.0) + 0.587 * (gcount - 1.0) + 0.114 * (bcount - 1.0));  // this spread value is optimised one -- try your own values for various effects!

    float scale = v_texCoords.y * (1.0 - parallax_size) + (parallax_size / 2.0) + (parallax * parallax_size / 2.0);


    float inR = mix(bottomColor.r, topColor.r, scale);
    float inG = mix(bottomColor.g, topColor.g, scale);
    float inB = mix(bottomColor.b, topColor.b, scale);

    vec4 inColor = vec4(inR, inG, inB, 1.0);

    vec2 entry = mod(gl_FragCoord.xy, vec2(bayerSize, bayerSize));

    gl_FragColor = nearestColour(inColor + spread * (bayer[int(entry.y) * int(bayerSize) + int(entry.x)] / bayerDivider - 0.5));
}

/*
UV mapping coord.y

-+ <- 1.0  =
D|         = // parallax of +1
i|  =      =
s|  = // parallax of 0
p|  =      =
.|         = // parallax of -1
-+ <- 0.0  =
*/
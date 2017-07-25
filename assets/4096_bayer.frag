#version 120
#ifdef GL_ES
    precision mediump float;
#endif


varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;


// "steps" of R, G and B. Must be integer && equal or greater than 2
uniform float rcount = 4.0;
uniform float gcount = 4.0;
uniform float bcount = 4.0;
uniform float acount = 1.0;



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


vec4 gammaIn(vec4 col) {
    return pow(col, vec4(2.2));
}

vec4 gammaOut(vec4 col) {
    return pow(col, vec4(1.0 / 2.2));
}

vec4 nearestColour(vec4 incolor) {
    vec4 rgbaCounts = vec4(rcount, gcount, bcount, acount);


    vec4 color = incolor;

    color.r = floor((rgbaCounts.r - 1.0) * color.r + 0.5) / (rgbaCounts.r - 1.0);
    color.g = floor((rgbaCounts.g - 1.0) * color.g + 0.5) / (rgbaCounts.g - 1.0);
    color.b = floor((rgbaCounts.b - 1.0) * color.b + 0.5) / (rgbaCounts.b - 1.0);

    if (rgbaCounts.a >= 2.0) {
        color.a = floor((rgbaCounts.a - 1.0) * color.a + 0.5) / (rgbaCounts.a - 1.0);
    }
    else {
        color.a = 1.0;
    }

    return (color);
}

void main(void) {
    float spread = 1.0 / (0.299 * (rcount - 1.0) + 0.587 * (gcount - 1.0) + 0.114 * (bcount - 1.0));  // this spread value is optimised one -- try your own values for various effects!


    // create texture coordinates based on pixelSize //
    vec4 inColor = (texture2D(u_texture, v_texCoords));

    vec2 entry = mod(gl_FragCoord.xy, vec2(bayerSize, bayerSize));

    gl_FragColor = nearestColour(inColor + spread * (bayer[int(entry.y) * int(bayerSize) + int(entry.x)] / bayerDivider - 0.5));
    //gl_FragColor = nearestColour(inColor);
}
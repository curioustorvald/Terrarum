#version 120
#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

//int bayer[14 * 14] = int[](131,187,8,78,50,18,134,89,155,102,29,95,184,73,22,86,113,171,142,105,34,166,9,60,151,128,40,110,168,137,45,28,64,188,82,54,124,189,80,13,156,56,7,61,186,121,154,6,108,177,24,100,38,176,93,123,83,148,96,17,88,133,44,145,69,161,139,72,30,181,115,27,163,47,178,65,164,14,120,48,5,127,153,52,190,58,126,81,116,21,106,77,173,92,191,63,99,12,76,144,4,185,37,149,192,39,135,23,117,31,170,132,35,172,103,66,129,79,3,97,57,159,70,141,53,94,114,20,49,158,19,146,169,122,183,11,104,180,2,165,152,87,182,118,91,42,67,25,84,147,43,85,125,68,16,136,71,10,193,112,160,138,51,111,162,26,194,46,174,107,41,143,33,74,1,101,195,15,75,140,109,90,32,62,157,98,167,119,179,59,36,130,175,55,0,150);
//float bayerSize = 14.0;
int bayer[4*4] = int[](0,8,2,10,12,4,14,6,3,11,1,9,15,7,13,5);
float bayerSize = 4.0;
float bayerDivider = bayerSize * bayerSize;

vec2 boolean = vec2(0.0, 1.0);

void main() {
    vec4 inColor = v_color * (texture2D(u_texture, v_texCoords));
    vec2 entry = mod(gl_FragCoord.xy, vec2(bayerSize, bayerSize));

    float bayerThreshold = bayer[int(entry.y) * int(bayerSize) + int(entry.x)] / bayerDivider;
    float alpha = inColor.a;

    vec4 selvec0 = vec4(0.0, 0.0, 0.0, (alpha > 1.0 / bayerDivider) ? 1.0 : 0.0);
    vec4 selvec = vec4(1.0, 1.0, 1.0, (alpha >= bayerThreshold) ? 1.0 : 0.0);

    vec4 out0 = inColor * boolean.yyyx + selvec0;
    gl_FragColor = out0 * selvec;
}
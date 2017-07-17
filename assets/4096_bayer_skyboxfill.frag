varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;


uniform vec3 topColor;
uniform vec3 bottomColor;


// "steps" of R, G and B. Must be integer && equal or greater than 2
uniform float rcount = 64.0; // it even works on 256.0!
uniform float gcount = 64.0; // using 64: has less banding and most monitors are internally 6-bit
uniform float bcount = 64.0;



int bayer[9][9] = {
{50,71,2,23,44,56,77,17,29},
{72,12,33,45,66,6,18,39,60},
{22,43,55,76,16,28,49,70,1},
{53,65,5,26,38,59,80,11,32},
{75,15,27,48,69,0,21,42,54},
{25,37,58,79,10,31,52,64,4},
{47,68,8,20,41,62,74,14,35},
{78,9,30,51,63,3,24,36,57},
{19,40,61,73,13,34,46,67,7}
};
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

    float scale = v_texCoords.y;
    float inR = mix(bottomColor.r, topColor.r, scale);
    float inG = mix(bottomColor.g, topColor.g, scale);
    float inB = mix(bottomColor.b, topColor.b, scale);

    vec4 inColor = vec4(inR, inG, inB, 1.0);

    vec2 entry = mod(gl_FragCoord.xy, vec2(bayerSize, bayerSize));

    gl_FragColor = nearestColour(inColor + spread * (bayer[int(entry.y)][int(entry.x)] / bayerDivider - 0.5));
}
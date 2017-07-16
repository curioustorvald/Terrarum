varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;


uniform vec3 topColor;
uniform vec3 bottomColor;


// "steps" of R, G and B. Must be integer && equal or greater than 2
uniform float rcount = 64.0; // it even works on 256.0!
uniform float gcount = 64.0; // using 128: has less banding and most monitors are internally 6-bit
uniform float bcount = 64.0;



int bayer[12][12] = {
{0  ,96 ,64 ,8  ,104,72 ,2  ,98 ,66 ,10 ,106,74 }, // 12x12 bayer ordered dithering
{112,80 ,16 ,120,88 ,24 ,114,82 ,18 ,122,90 ,26 }, // pattern. Each input pixel
{48 ,32 ,128,56 ,40 ,136,50 ,34 ,130,58 ,42 ,138}, // is scaled to the 0..143 range
{12 ,108,76 ,4  ,100,68 ,14 ,110,78 ,6  ,102,70 }, // before looking in this table
{124,92 ,28 ,116,84 ,20 ,126,94 ,30 ,118,86 ,22 }, // to determine the action
{60 ,44 ,140,52 ,36 ,132,62 ,46 ,142,54 ,38 ,134},
{3  ,99 ,67 ,11 ,107,75 ,1  ,97 ,65 ,9  ,105,73 },
{115,83 ,19 ,123,91 ,27 ,113,81 ,17 ,121,89 ,25 },
{51 ,35 ,131,59 ,43 ,139,49 ,33 ,129,57 ,41 ,137},
{15 ,111,79 ,7  ,103,71 ,13 ,109,77 ,5  ,101,69 },
{127,95 ,31 ,119,87 ,23 ,125,93 ,29 ,117,85 ,21 },
{63 ,47 ,143,55 ,39 ,135,61 ,45 ,141,53 ,37 ,133}}; // fun fact: you can calculate bayer value on-the-fly but LUT is faster
float bayerSize = 12.0;
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
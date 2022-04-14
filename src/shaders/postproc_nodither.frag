/**
 * Blue Noise texture created by Christoph Peters, released under CC0
 * http://momentsingraphics.de/BlueNoise.html
 */

#version 130
#ifdef GL_ES
precision mediump float;
#endif


vec4 gammaIn(vec4 col) {
    return pow(col, vec4(2.2));
}

vec4 gammaOut(vec4 col) {
    return pow(col, vec4(1.0 / 2.2));
}

varying vec4 v_color; // unused!
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform sampler2D u_pattern;
uniform ivec2 rnd = ivec2(0,0);

uniform float quant = 255.0; // 64 steps -> 63.0; 256 steps -> 255.0

vec2 boolean = vec2(0.0, 1.0);

mat4 rgb_to_ycocg = mat4(
    0.25,  1.0, -0.5, 0.0,
     0.5,  0.0,  1.0, 0.0,
    0.25, -1.0, -0.5, 0.0,
     0.0,  0.0,  0.0, 1.0
);

mat4 ycocg_to_rgb = mat4(
     1.0, 1.0,  1.0, 0.0,
     0.5, 0.0, -0.5, 0.0,
    -0.5, 0.5, -0.5, 0.0,
     0.0, 0.0,  0.0, 1.0
);


uniform vec4 vibrancy = vec4(1.0);//vec4(1.0, 1.4, 1.2, 1.0);

void main(void) {
    // convert input RGB into YCoCg
    vec4 incolour = texture2D(u_texture, v_texCoords);
    vec4 yog = rgb_to_ycocg * incolour; // vec4(Y, Co, Cg, A) where Y,A=[0,1]; Co,Cg=[-1,1]

    // Do colour-grading magic
    vec4 sgn = sign(yog);
    vec4 absval = abs(yog);
    vec4 raised = pow(absval, boolean.yyyy / vibrancy);
    vec4 newColour = sgn * raised;

    // Dither the output
    vec4 graded = ycocg_to_rgb * newColour;
    gl_FragColor = graded * boolean.yyyx + boolean.xxxy; // use quantised RGB but not the A
}
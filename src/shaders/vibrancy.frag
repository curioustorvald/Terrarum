
#ifdef GL_ES
precision mediump float;
#endif


vec4 gammaIn(vec4 col) {
    return pow(col, vec4(2.2));
}

vec4 gammaOut(vec4 col) {
    return pow(col, vec4(1.0 / 2.2));
}

in vec4 v_color; // unused!
in vec2 v_texCoords;

uniform sampler2D u_texture;

const vec2 boolean = vec2(0.0, 1.0);

const mat4 rgb_to_ycocg = mat4(
0.25,  1.0, -0.5, 0.0,
0.5,  0.0,  1.0, 0.0,
0.25, -1.0, -0.5, 0.0,
0.0,  0.0,  0.0, 1.0
);

const mat4 ycocg_to_rgb = mat4(
1.0, 1.0,  1.0, 0.0,
0.5, 0.0, -0.5, 0.0,
-0.5, 0.5, -0.5, 0.0,
0.0, 0.0,  0.0, 1.0
);


uniform vec4 vibrancy = vec4(1.0);//vec4(1.0, 1.4, 1.2, 1.0);

out vec4 fragColor;

void main(void) {
    // convert input RGB into YCoCg
    vec4 incolour = texture(u_texture, v_texCoords);
    vec4 yog = rgb_to_ycocg * incolour; // vec4(Y, Co, Cg, A) where Y,A=[0,1]; Co,Cg=[-1,1]

    // Do colour-grading magic
    vec4 sgn = sign(yog);
    vec4 absval = abs(yog);
    vec4 raised = pow(absval, boolean.yyyy / vibrancy);
    vec4 newColour = sgn * raised;

    // Dither the output
    vec4 graded = ycocg_to_rgb * newColour;
    fragColor = fma(graded, boolean.yyyx, boolean.xxxy); // use quantised RGB but not the A
}
#ifdef GL_ES
precision mediump float;
#endif


varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

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

vec2 boolean = vec2(0.0, 1.0);

void main() {
    vec4 incolour = texture2D(u_texture, v_texCoords);
    vec4 yog = rgb_to_ycocg * incolour; // vec4(Y, Co, Cg, A) where Y,A=[0,1]; Co,Cg=[-1,1]

    vec4 scalar = vec4(1.0, 2.0, 2.0, 1.0);

    gl_FragColor = ycocg_to_rgb * (yog * scalar);
}
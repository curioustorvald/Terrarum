#version 120
#ifdef GL_ES
    precision mediump float;
#endif


varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;


uniform vec3 topColor;
uniform vec3 bottomColor;
uniform float parallax = 0.0; // +1.0: all top col, -1.0: all bototm col, 0.0: normal grad
uniform float parallax_size = 1.0/3.0; // 0: no parallax



void main(void) {
    float scale = v_texCoords.y * (1.0 - parallax_size) + (parallax_size / 2.0) + (parallax * parallax_size / 2.0);

    float inR = mix(bottomColor.r, topColor.r, scale);
    float inG = mix(bottomColor.g, topColor.g, scale);
    float inB = mix(bottomColor.b, topColor.b, scale);

    gl_FragColor = vec4(inR, inG, inB, 1.0);
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
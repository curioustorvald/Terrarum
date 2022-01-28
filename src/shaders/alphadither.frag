#version 120
#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

int bayer[36] = int[](
192,78,21,120,163,14,
234,142,184,248,56,106,
64,7,92,35,149,206,
128,170,220,199,85,28,
241,49,113,0,135,177,
42,156,213,71,227,99
);
float bayerSize = 6.0;
float bayerDivider = 256;

vec2 boolean = vec2(0.0, 1.0);

void main() {
    vec4 inColor = v_color * (texture2D(u_texture, v_texCoords));
    vec2 entry = mod(gl_FragCoord.xy, vec2(bayerSize, bayerSize));

    float bayerThreshold = float(bayer[int(entry.y) * int(bayerSize) + int(entry.x)]) / bayerDivider;
    float alpha = inColor.a;

    vec4 selvec = vec4(0.0, 0.0, 0.0, (alpha > bayerThreshold) ? 1.0 : 0.0);

    gl_FragColor = inColor * boolean.yyyx + selvec;
//    gl_FragColor = inColor;
}
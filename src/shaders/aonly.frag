varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

vec2 boolean = vec2(0.0, 1.0);

void main(void) {
    gl_FragColor = texture2D(u_texture, v_texCoords).aaaa * boolean.yyyx + boolean.xxxy;
}
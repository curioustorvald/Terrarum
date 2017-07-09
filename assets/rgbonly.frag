varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

void main(void) {
    vec3 color = texture2D(u_texture, v_texCoords).rgb;
    gl_FragColor = vec4(color, 1.0);
}
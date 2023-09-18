
#ifdef GL_ES
precision mediump float;
#endif

in vec4 v_col;

out vec4 fragColor;

void main() {
    fragColor = v_col;
}
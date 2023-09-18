#ifdef GL_ES
    precision mediump float;
#endif

in vec4 v_color;
in vec2 v_texCoords;

uniform sampler2D u_texture;

uniform float rcount = 64.0;
uniform float gcount = 64.0;
uniform float bcount = 64.0;
uniform float acount = 1.0;

uniform vec2 circleCentrePoint;
uniform vec2 colorCentrePoint;
uniform float circleSize;

out vec4 fragColor;

void main() {
    vec2 screenCoord = gl_FragCoord.xy;

    float distToCircleCentre =
            (screenCoord.x - circleCentrePoint.x + 0.5) * (screenCoord.x - circleCentrePoint.x + 0.5) +
            (screenCoord.y - circleCentrePoint.y + 0.5) * (screenCoord.y - circleCentrePoint.y + 0.5);
    float circleSizeSqr = circleSize * circleSize / 4;


    if (distToCircleCentre <= circleSizeSqr) {
    	fragColor = vec4(0.993, 0.993, 0.993, 1.0);
    }
    else if (distToCircleCentre <= circleSizeSqr + 200) { // dunno why it's 200; 2000 makes 10px feather
    	fragColor = vec4(0.993, 0.993, 0.993, 1 - (distToCircleCentre - circleSizeSqr) / 200);
    }
    else {
        fragColor = vec4(0,0,0,1);
    }

}

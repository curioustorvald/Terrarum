#version 120
#ifdef GL_ES
    precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

// all 3 must have the same dimension!
// the divisor of 2 input and an output must be the same. I.e. either divide all by 4, or not.
uniform sampler2D shades;
uniform sampler2D lights;
// WARNING -- Gdx.Color.toIntBits returns ABGR, but GLSL expects RGBA. Use the function Color.toRGBA() in LightmapRenderNew
uniform sampler2D u_texture;
uniform vec2 outSize;
uniform float multiplier = 4.0; // if divided by four, put 4.0 in there

#define TRAVERSE_SIZE 128 // should be good for screen size up to 1920 for tile size of 16

vec4 sampleFrom(sampler2D from, vec2 which) {
    return texture2D(from, which / outSize);
}

int traceRayCount(vec2 delta) {
    vec2 absDelta = abs(delta);
    int arraySize = int(max(absDelta.x, absDelta.y));
    return arraySize + 1;
}

vec2[TRAVERSE_SIZE] traceRay(int arraySize, vec2 from, vec2 to) {
    vec2 delta = to - from;
    vec2[TRAVERSE_SIZE] returnArray;
    int arri = 0;

    // if the line is not vertical...
    if (delta.x != 0) {
        float deltaError = abs(delta.y / delta.x);
        float error = 0.0;
        float traceY = from.y;

        for (float traceX = from.x; traceX <= to.x; traceX++) {
            // plot(traceX, traceY)
            returnArray[arri] = vec2(traceX, traceY);
            arri = arri + 1;

            error = error + deltaError;
            if (error >= 0.5) {
                traceY = traceY + sign(delta.y);
                error = error - 1.0;
            }
        }
    }
    else {
        for (float traceY = from.y; traceY <= to.y; traceY++) {
            returnArray[arri] = vec2(from.x, traceY);
        }
    }

    return returnArray;
}

void main() {

    // this code will produce y-flipped image. It's your job to flip it again (e.g. using y-flipped fullscreen quad)

    // Nice try, but it kills GPU :(
    // reason: looks like traceRayCount() returns value greater than TRAVERSE_SIZE.
    //         even if I make traceRayCount() to return constant 3, I get less than 1 fps on GTX 970.

    vec4 outColor = vec4(0.0,0.0,0.0,0.0);

    // 1. pick a light source
    for (int y = 0; y < int(outSize.y); y++) {
        for (int x = 0; x < int(outSize.x); x++) {
            vec2 from = vec2(x + 0.5, y + 0.5); // +0.5 is used because gl_FragCoord does
            vec2 to = gl_FragCoord.xy;
            vec2 delta = to - from;
            int traceCount = traceRayCount(delta);
            vec4 light = sampleFrom(lights, from);

            // 2. get a trace path
            vec2[TRAVERSE_SIZE] returnArray = traceRay(traceCount, from, to);

            // 2.1 get angular darkening coefficient
            vec2 unitVec = delta / max(delta.x, delta.y);
            float angularDimming = sqrt(unitVec.x * unitVec.x + unitVec.y * unitVec.y);
            //float angularDimming = 1.0; // TODO depends on the angle of (lightPos, gl_FragCoord.x)

            // 3. traverse the light path to dim the "light"
            // var "light" will be attenuated after this loop
            for (int i = 0; i < traceCount; i++) {
                vec4 shade = sampleFrom(shades, returnArray[i]) * angularDimming;

                light = light - shade;
            }

            // 4. mix the incoming light into the light buffer.
            outColor = max(outColor, light);
        }
    }

    gl_FragColor = outColor * multiplier;
    //gl_FragColor = vec4(0,1,0,1);

    //gl_FragColor = sampleFrom(lights, gl_FragCoord.xy) * multiplier;


}

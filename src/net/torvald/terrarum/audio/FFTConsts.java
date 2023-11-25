package net.torvald.terrarum.audio;

/**
 * Created by minjaesong on 2023-11-25.
 */
public class FFTConsts {

    // org.apache.commons.math3.transform.FastFouriesTransformer.java:58
    /**
     * {@code W_SUB_N_R[i]} is the real part of
     * {@code exp(- 2 * i * pi / n)}:
     * {@code W_SUB_N_R[i] = cos(2 * pi/ n)}, where {@code n = 2^i}.
     */
    public static final double[] W_SUB_N_R =
            {  0x1.0p0, -0x1.0p0, 0x1.1a62633145c07p-54, 0x1.6a09e667f3bcdp-1
                    , 0x1.d906bcf328d46p-1, 0x1.f6297cff75cbp-1, 0x1.fd88da3d12526p-1, 0x1.ff621e3796d7ep-1
                    , 0x1.ffd886084cd0dp-1, 0x1.fff62169b92dbp-1, 0x1.fffd8858e8a92p-1, 0x1.ffff621621d02p-1
                    , 0x1.ffffd88586ee6p-1, 0x1.fffff62161a34p-1, 0x1.fffffd8858675p-1, 0x1.ffffff621619cp-1
                    , 0x1.ffffffd885867p-1, 0x1.fffffff62161ap-1, 0x1.fffffffd88586p-1, 0x1.ffffffff62162p-1
                    , 0x1.ffffffffd8858p-1, 0x1.fffffffff6216p-1, 0x1.fffffffffd886p-1, 0x1.ffffffffff621p-1
                    , 0x1.ffffffffffd88p-1, 0x1.fffffffffff62p-1, 0x1.fffffffffffd9p-1, 0x1.ffffffffffff6p-1
                    , 0x1.ffffffffffffep-1, 0x1.fffffffffffffp-1, 0x1.0p0, 0x1.0p0
                    , 0x1.0p0, 0x1.0p0, 0x1.0p0, 0x1.0p0
                    , 0x1.0p0, 0x1.0p0, 0x1.0p0, 0x1.0p0
                    , 0x1.0p0, 0x1.0p0, 0x1.0p0, 0x1.0p0
                    , 0x1.0p0, 0x1.0p0, 0x1.0p0, 0x1.0p0
                    , 0x1.0p0, 0x1.0p0, 0x1.0p0, 0x1.0p0
                    , 0x1.0p0, 0x1.0p0, 0x1.0p0, 0x1.0p0
                    , 0x1.0p0, 0x1.0p0, 0x1.0p0, 0x1.0p0
                    , 0x1.0p0, 0x1.0p0, 0x1.0p0 };

    /**
     * {@code W_SUB_N_I[i]} is the imaginary part of
     * {@code exp(- 2 * i * pi / n)}:
     * {@code W_SUB_N_I[i] = -sin(2 * pi/ n)}, where {@code n = 2^i}.
     */
    public static final double[] W_SUB_N_I =
            {  0x1.1a62633145c07p-52, -0x1.1a62633145c07p-53, -0x1.0p0, -0x1.6a09e667f3bccp-1
                    , -0x1.87de2a6aea963p-2, -0x1.8f8b83c69a60ap-3, -0x1.917a6bc29b42cp-4, -0x1.91f65f10dd814p-5
                    , -0x1.92155f7a3667ep-6, -0x1.921d1fcdec784p-7, -0x1.921f0fe670071p-8, -0x1.921f8becca4bap-9
                    , -0x1.921faaee6472dp-10, -0x1.921fb2aecb36p-11, -0x1.921fb49ee4ea6p-12, -0x1.921fb51aeb57bp-13
                    , -0x1.921fb539ecf31p-14, -0x1.921fb541ad59ep-15, -0x1.921fb5439d73ap-16, -0x1.921fb544197ap-17
                    , -0x1.921fb544387bap-18, -0x1.921fb544403c1p-19, -0x1.921fb544422c2p-20, -0x1.921fb54442a83p-21
                    , -0x1.921fb54442c73p-22, -0x1.921fb54442cefp-23, -0x1.921fb54442d0ep-24, -0x1.921fb54442d15p-25
                    , -0x1.921fb54442d17p-26, -0x1.921fb54442d18p-27, -0x1.921fb54442d18p-28, -0x1.921fb54442d18p-29
                    , -0x1.921fb54442d18p-30, -0x1.921fb54442d18p-31, -0x1.921fb54442d18p-32, -0x1.921fb54442d18p-33
                    , -0x1.921fb54442d18p-34, -0x1.921fb54442d18p-35, -0x1.921fb54442d18p-36, -0x1.921fb54442d18p-37
                    , -0x1.921fb54442d18p-38, -0x1.921fb54442d18p-39, -0x1.921fb54442d18p-40, -0x1.921fb54442d18p-41
                    , -0x1.921fb54442d18p-42, -0x1.921fb54442d18p-43, -0x1.921fb54442d18p-44, -0x1.921fb54442d18p-45
                    , -0x1.921fb54442d18p-46, -0x1.921fb54442d18p-47, -0x1.921fb54442d18p-48, -0x1.921fb54442d18p-49
                    , -0x1.921fb54442d18p-50, -0x1.921fb54442d18p-51, -0x1.921fb54442d18p-52, -0x1.921fb54442d18p-53
                    , -0x1.921fb54442d18p-54, -0x1.921fb54442d18p-55, -0x1.921fb54442d18p-56, -0x1.921fb54442d18p-57
                    , -0x1.921fb54442d18p-58, -0x1.921fb54442d18p-59, -0x1.921fb54442d18p-60 };

}

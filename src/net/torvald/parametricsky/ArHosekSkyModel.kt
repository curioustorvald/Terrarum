package net.torvald.parametricsky

import net.torvald.parametricsky.datasets.DatasetCIEXYZ.datasetsXYZ
import net.torvald.parametricsky.datasets.DatasetCIEXYZ.datasetsXYZRad
import net.torvald.parametricsky.datasets.DatasetRGB.datasetsRGB
import net.torvald.parametricsky.datasets.DatasetRGB.datasetsRGBRad
import net.torvald.parametricsky.datasets.DatasetSpectral.datasets
import net.torvald.parametricsky.datasets.DatasetSpectral.datasetsRad
import net.torvald.parametricsky.datasets.DatasetSpectral.limbDarkeningDatasets
import net.torvald.parametricsky.datasets.DatasetSpectral.solarDatasets
import kotlin.math.*
import kotlin.test.assertEquals

/*
This source is published under the following 3-clause BSD license.

Copyright (c) 2012 - 2013, Lukas Hosek and Alexander Wilkie
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * None of the names of the contributors may be used to endorse or promote
      products derived from this software without specific prior written
      permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/* ============================================================================

This file is part of a sample implementation of the analytical skylight and
solar radiance models presented in the SIGGRAPH 2012 paper


           "An Analytic Model for Full Spectral Sky-Dome Radiance"

and the 2013 IEEE CG&A paper

       "Adding a Solar Radiance Function to the Hosek Skylight Model"

                                   both by

                       Lukas Hosek and Alexander Wilkie
                Charles University in Prague, Czech Republic


                        Version: 1.4a, February 22nd, 2013

Version history:

1.4a  February 22nd, 2013
      Removed unnecessary and counter-intuitive solar radius parameters
      from the interface of the colourspace sky dome initialisation functions.

1.4   February 11th, 2013
      Fixed a bug which caused the relative brightness of the solar disc
      and the sky dome to be off by a factor of about 6. The sun was too
      bright: this affected both normal and alien sun scenarios. The
      coefficients of the solar radiance function were changed to fix this.

1.3   January 21st, 2013 (not released to the public)
      Added support for solar discs that are not exactly the same size as
      the terrestrial sun. Also added support for suns with a different
      emission spectrum ("Alien World" functionality).

1.2a  December 18th, 2012
      Fixed a mistake and some inaccuracies in the solar radiance function
      explanations found in ArHosekSkyModel.h. The actual source code is
      unchanged compared to version 1.2.

1.2   December 17th, 2012
      Native RGB data and a solar radiance function that matches the turbidity
      conditions were added.

1.1   September 2012
      The coefficients of the spectral model are now scaled so that the output
      is given in physical units: W / (m^-2 * sr * nm). Also, the output of the
      XYZ model is now no longer scaled to the range [0...1]. Instead, it is
      the result of a simple conversion from spectral data via the CIE 2 degree
      standard observer matching functions. Therefore, after multiplication
      with 683 lm / W, the Y channel now corresponds to luminance in lm.

1.0   May 11th, 2012
      Initial release.


Please visit http://cgg.mff.cuni.cz/projects/SkylightModelling/ to check if
an updated version of this code has been published!

============================================================================ */

/*

All instructions on how to use this code are in the accompanying header file.

*/

//   Some macro definitions that occur elsewhere in ART, and that have to be
//   replicated to make this a stand-alone module.

typealias ArHosekSkyModel_Dataset = DoubleArray
typealias ArHosekSkyModel_Radiance_Dataset = DoubleArray
class ArHosekSkyModelConfiguration(val data: DoubleArray) {
    init {
        assertEquals(9, data.size, "Data size does not match: expected: 9, actual: ${data.size}")
    }
    operator fun get(i: Int) = data[i]
    operator fun set(i: Int, value: Double) { data[i] = value }
}

data class ArHosekSkyModelState(
        val configs: Array<ArHosekSkyModelConfiguration>,
        val radiances: DoubleArray,
        val turbidity: Double,
        val solar_radius: Double,
        val emission_correction_factor_sky: DoubleArray,
        val emission_correction_factor_sun: DoubleArray,
        val albedo: Double,
        val elevation: Double
) {
    init {
        assertEquals(11, configs.size, "Size of configs does not match: expected: 11, actual: ${configs.size}")
        assertEquals(11, radiances.size, "Size of radiances does not match: expected: 11, actual: ${radiances.size}")
        assertEquals(11, emission_correction_factor_sky.size, "Size of emission_correction_factor_sky does not match: expected: 11, actual: ${emission_correction_factor_sky.size}")
        assertEquals(11, emission_correction_factor_sun.size, "Size of emission_correction_factor_sun does not match: expected: 11, actual: ${emission_correction_factor_sun.size}")
    }
}

private fun generateNullArHosekSkyModelConfigurations() = Array<ArHosekSkyModelConfiguration>(11) {
    ArHosekSkyModelConfiguration(DoubleArray(9))
}

private fun fmod(a: Double, b: Double) = Math.floorMod(a.toInt(), b.toInt())

object ArHosekSkyModel {
    private val MATH_DEG_TO_RAD = Math.PI / 180.0
    private val MATH_RAD_TO_DEG = 180.0 / Math.PI

    private fun Double.DEGREES() = this * MATH_DEG_TO_RAD

    private const val NIL = 0
    private const val MATH_PI = Math.PI
    private val TERRESTRIAL_SOLAR_RADIUS = ((0.51.DEGREES()) / 2.0)

// internal definitions

    private class DoubleArrayPtr(val arr: DoubleArray, var ptrOffset: Int) {
        operator fun get(i: Int) = arr[ptrOffset + i]

    }

    private class DoubleArrayArrayPtr(var arr: Array<DoubleArray>, var ptrOffset: Int) {
        fun dec(): DoubleArrayArrayPtr {
            ptrOffset -= 1
            return this
        }

        private val width = arr[0].size // assuming solarDataset320..solarDataset720 to have exactly the same sizes
        operator fun get(i: Int) = arr[ptrOffset / width][ptrOffset % width]
    }

// internal functions

    private inline fun pow(a: Double, b: Double) = a.pow(b)

    private fun ArHosekSkyModel_CookConfiguration(
            dataset: ArHosekSkyModel_Dataset,
            config: ArHosekSkyModelConfiguration,
            turbidity: Double,
            albedo: Double,
            solar_elevation: Double
    ) {
        var elev_matrix: DoubleArrayPtr

        val int_turbidity = turbidity.toInt()
        val turbidity_rem = turbidity - int_turbidity.toDouble()

        val solar_elevation = pow(solar_elevation / (MATH_PI / 2.0), (1.0 / 3.0))

        // alb 0 low turb

        elev_matrix = DoubleArrayPtr(dataset, 9 * 6 * (int_turbidity - 1))


        for (i in 0 until 9) {
            //(1-t).^3* A1 + 3*(1-t).^2.*t * A2 + 3*(1-t) .* t .^ 2 * A3 + t.^3 * A4;
            config[i] =
                    (1.0 - albedo) * (1.0 - turbidity_rem) *
                    (pow(1.0 - solar_elevation, 5.0) * elev_matrix[i] +
                     5.0 * pow(1.0 - solar_elevation, 4.0) * solar_elevation * elev_matrix[i + 9] +
                     10.0 * pow(1.0 - solar_elevation, 3.0) * pow(solar_elevation, 2.0) * elev_matrix[i + 18] +
                     10.0 * pow(1.0 - solar_elevation, 2.0) * pow(solar_elevation, 3.0) * elev_matrix[i + 27] +
                     5.0 * (1.0 - solar_elevation) * pow(solar_elevation, 4.0) * elev_matrix[i + 36] +
                     pow(solar_elevation, 5.0) * elev_matrix[i + 45])
        }

        // alb 1 low turb
        elev_matrix = DoubleArrayPtr(dataset, 9 * 6 * 10 + 9 * 6 * (int_turbidity - 1))
        for (i in 0 until 9) {
            //(1-t).^3* A1 + 3*(1-t).^2.*t * A2 + 3*(1-t) .* t .^ 2 * A3 + t.^3 * A4;
            config[i] +=
                    (albedo) * (1.0 - turbidity_rem) *
                    (pow(1.0 - solar_elevation, 5.0) * elev_matrix[i] +
                     5.0 * pow(1.0 - solar_elevation, 4.0) * solar_elevation * elev_matrix[i + 9] +
                     10.0 * pow(1.0 - solar_elevation, 3.0) * pow(solar_elevation, 2.0) * elev_matrix[i + 18] +
                     10.0 * pow(1.0 - solar_elevation, 2.0) * pow(solar_elevation, 3.0) * elev_matrix[i + 27] +
                     5.0 * (1.0 - solar_elevation) * pow(solar_elevation, 4.0) * elev_matrix[i + 36] +
                     pow(solar_elevation, 5.0) * elev_matrix[i + 45])
        }

        if (int_turbidity == 10) return

        // alb 0 high turb
        elev_matrix = DoubleArrayPtr(dataset, 9 * 6 * (int_turbidity))
        for (i in 0 until 9)
        {
            //(1-t).^3* A1 + 3*(1-t).^2.*t * A2 + 3*(1-t) .* t .^ 2 * A3 + t.^3 * A4;
            config[i] +=
                    (1.0 - albedo) * (turbidity_rem) *
                    (pow(1.0 - solar_elevation, 5.0) * elev_matrix[i] +
                     5.0 * pow(1.0 - solar_elevation, 4.0) * solar_elevation * elev_matrix[i + 9] +
                     10.0 * pow(1.0 - solar_elevation, 3.0) * pow(solar_elevation, 2.0) * elev_matrix[i + 18] +
                     10.0 * pow(1.0 - solar_elevation, 2.0) * pow(solar_elevation, 3.0) * elev_matrix[i + 27] +
                     5.0 * (1.0 - solar_elevation) * pow(solar_elevation, 4.0) * elev_matrix[i + 36] +
                     pow(solar_elevation, 5.0) * elev_matrix[i + 45])
        }

        // alb 1 high turb
        elev_matrix = DoubleArrayPtr(dataset, 9 * 6 * 10 + 9 * 6 * (int_turbidity))
        for (i in 0 until 9)
        {
            //(1-t).^3* A1 + 3*(1-t).^2.*t * A2 + 3*(1-t) .* t .^ 2 * A3 + t.^3 * A4;
            config[i] +=
                    (albedo) * (turbidity_rem) *
                    (pow(1.0 - solar_elevation, 5.0) * elev_matrix[i] +
                     5.0 * pow(1.0 - solar_elevation, 4.0) * solar_elevation * elev_matrix[i + 9] +
                     10.0 * pow(1.0 - solar_elevation, 3.0) * pow(solar_elevation, 2.0) * elev_matrix[i + 18] +
                     10.0 * pow(1.0 - solar_elevation, 2.0) * pow(solar_elevation, 3.0) * elev_matrix[i + 27] +
                     5.0 * (1.0 - solar_elevation) * pow(solar_elevation, 4.0) * elev_matrix[i + 36] +
                     pow(solar_elevation, 5.0) * elev_matrix[i + 45]);
        }
    }

    private fun ArHosekSkyModel_CookRadianceConfiguration(
            dataset: ArHosekSkyModel_Radiance_Dataset,
            turbidity: Double,
            albedo: Double,
            solar_elevation: Double
    ): Double {
        var elev_matrix: DoubleArrayPtr

        val int_turbidity = turbidity.toInt()
        val turbidity_rem = turbidity - int_turbidity.toDouble()
        var res: Double

        val solar_elevation = pow(solar_elevation / (MATH_PI / 2.0), (1.0 / 3.0))

        // alb 0 low turb
        elev_matrix = DoubleArrayPtr(dataset, 6 * (int_turbidity - 1))
        //(1-t).^3* A1 + 3*(1-t).^2.*t * A2 + 3*(1-t) .* t .^ 2 * A3 + t.^3 * A4;
        res = (1.0 - albedo) * (1.0 - turbidity_rem) *
              (pow(1.0 - solar_elevation, 5.0) * elev_matrix[0] +
               5.0 * pow(1.0 - solar_elevation, 4.0) * solar_elevation * elev_matrix[1] +
               10.0 * pow(1.0 - solar_elevation, 3.0) * pow(solar_elevation, 2.0) * elev_matrix[2] +
               10.0 * pow(1.0 - solar_elevation, 2.0) * pow(solar_elevation, 3.0) * elev_matrix[3] +
               5.0 * (1.0 - solar_elevation) * pow(solar_elevation, 4.0) * elev_matrix[4] +
               pow(solar_elevation, 5.0) * elev_matrix[5])

        // alb 1 low turb
        elev_matrix = DoubleArrayPtr(dataset, 6 * 10 + 6 * (int_turbidity - 1))
        //(1-t).^3* A1 + 3*(1-t).^2.*t * A2 + 3*(1-t) .* t .^ 2 * A3 + t.^3 * A4;
        res += (albedo) * (1.0 - turbidity_rem) *
               (pow(1.0 - solar_elevation, 5.0) * elev_matrix[0] +
                5.0 * pow(1.0 - solar_elevation, 4.0) * solar_elevation * elev_matrix[1] +
                10.0 * pow(1.0 - solar_elevation, 3.0) * pow(solar_elevation, 2.0) * elev_matrix[2] +
                10.0 * pow(1.0 - solar_elevation, 2.0) * pow(solar_elevation, 3.0) * elev_matrix[3] +
                5.0 * (1.0 - solar_elevation) * pow(solar_elevation, 4.0) * elev_matrix[4] +
                pow(solar_elevation, 5.0) * elev_matrix[5])

        if (int_turbidity == 10) return res

        // alb 0 high turb
        elev_matrix = DoubleArrayPtr(dataset, 6 * (int_turbidity))
        //(1-t).^3* A1 + 3*(1-t).^2.*t * A2 + 3*(1-t) .* t .^ 2 * A3 + t.^3 * A4;
        res += (1.0 - albedo) * (turbidity_rem) *
               (pow(1.0 - solar_elevation, 5.0) * elev_matrix[0] +
                5.0 * pow(1.0 - solar_elevation, 4.0) * solar_elevation * elev_matrix[1] +
                10.0 * pow(1.0 - solar_elevation, 3.0) * pow(solar_elevation, 2.0) * elev_matrix[2] +
                10.0 * pow(1.0 - solar_elevation, 2.0) * pow(solar_elevation, 3.0) * elev_matrix[3] +
                5.0 * (1.0 - solar_elevation) * pow(solar_elevation, 4.0) * elev_matrix[4] +
                pow(solar_elevation, 5.0) * elev_matrix[5])

        // alb 1 high turb
        elev_matrix = DoubleArrayPtr(dataset, 6 * 10 + 6 * (int_turbidity))
        //(1-t).^3* A1 + 3*(1-t).^2.*t * A2 + 3*(1-t) .* t .^ 2 * A3 + t.^3 * A4;
        res += (albedo) * (turbidity_rem) *
               (pow(1.0 - solar_elevation, 5.0) * elev_matrix[0] +
                5.0 * pow(1.0 - solar_elevation, 4.0) * solar_elevation * elev_matrix[1] +
                10.0 * pow(1.0 - solar_elevation, 3.0) * pow(solar_elevation, 2.0) * elev_matrix[2] +
                10.0 * pow(1.0 - solar_elevation, 2.0) * pow(solar_elevation, 3.0) * elev_matrix[3] +
                5.0 * (1.0 - solar_elevation) * pow(solar_elevation, 4.0) * elev_matrix[4] +
                pow(solar_elevation, 5.0) * elev_matrix[5])

        return res
    }

    private fun ArHosekSkyModel_GetRadianceInternal(
            configuration: ArHosekSkyModelConfiguration,
            theta: Double,
            gamma: Double
    ): Double {
        val expM = exp(configuration[4] * gamma)
        val rayM = cos(gamma) * cos(gamma)
        val mieM = (1.0 + cos(gamma) * cos(gamma)) / pow((1.0 + configuration[8] * configuration[8] - 2.0 * configuration[8] * cos(gamma)), 1.5)
        val zenith = sqrt(cos(theta))

        return (1.0 + configuration[0] * exp(configuration[1] / (cos(theta) + 0.01))) *
               (configuration[2] + configuration[3] * expM + configuration[5] * rayM + configuration[6] * mieM + configuration[7] * zenith)
    }

// spectral version

    fun arhosekskymodelstate_alloc_init(
            solar_elevation: Double,
            atmospheric_turbidity: Double,
            ground_albedo: Double
    ): ArHosekSkyModelState {
        val solar_radius = (0.51.DEGREES()) / 2.0
        val turbidity = atmospheric_turbidity
        val albedo = ground_albedo
        val elevation = solar_elevation

        val ret_radiances = DoubleArray(11)
        val ret_emission_correction_factor_sun = DoubleArray(11)
        val ret_emission_correction_factor_sky = DoubleArray(11)
        val ret_configs = generateNullArHosekSkyModelConfigurations()

        for (wl in 0 until 11) {
            ArHosekSkyModel_CookConfiguration(
                    datasets[wl],
                    ret_configs[wl],
                    atmospheric_turbidity,
                    ground_albedo,
                    solar_elevation
            )

            ret_radiances[wl] =
                    ArHosekSkyModel_CookRadianceConfiguration(
                            datasetsRad[wl],
                            atmospheric_turbidity,
                            ground_albedo,
                            solar_elevation
                    )

            ret_emission_correction_factor_sun[wl] = 1.0
            ret_emission_correction_factor_sky[wl] = 1.0
        }

        return ArHosekSkyModelState(
                ret_configs,
                ret_radiances,
                turbidity,
                solar_radius,
                ret_emission_correction_factor_sky,
                ret_emission_correction_factor_sun,
                albedo, elevation
        )
    }

//   'blackbody_scaling_factor'
//
//   Fudge factor, computed in Mathematica, to scale the results of the
//   following function to match the solar radiance spectrum used in the
//   original simulation. The scaling is done so their integrals over the
//   range from 380.0 to 720.0 nanometers match for a blackbody temperature
//   of 5800 K.
//   Which leaves the original spectrum being less bright overall than the 5.8k
//   blackbody radiation curve if the ultra-violet part of the spectrum is
//   also considered. But the visible brightness should be very similar.

    private const val blackbody_scaling_factor = 3.19992 * 10E-11

//   'art_blackbody_dd_value()' function
//
//   Blackbody radiance, Planck's formula

    private fun art_blackbody_dd_value(temperature: Double, lambda: Double): Double {
        val c1 = 3.74177 * 10E-17
        val c2 = 0.0143878
        return (c1 / (pow(lambda, 5.0))) * (1.0 / (exp(c2 / (lambda * temperature)) - 1.0))
    }

//   'originalSolarRadianceTable[]'
//
//   The solar spectrum incident at the top of the atmosphere, as it was used
//   in the brute force path tracer that generated the reference results the
//   model was fitted to. We need this as the yardstick to compare any altered
//   Blackbody emission spectra for alien world stars to.

//   This is just the data from the Preetham paper, extended into the UV range.

    private val originalSolarRadianceTable = doubleArrayOf(
        7500.0,
        12500.0,
        21127.5,
        26760.5,
        30663.7,
        27825.0,
        25503.8,
        25134.2,
        23212.1,
        21526.7,
        19870.8
    )

    fun arhosekskymodelstate_alienworld_alloc_init(
            solar_elevation: Double,
            solar_intensity: Double,
            solar_surface_temperature_kelvin: Double,
            atmospheric_turbidity: Double,
            ground_albedo: Double
    ): ArHosekSkyModelState {
        val turbidity = atmospheric_turbidity
        val albedo = ground_albedo
        val elevation = solar_elevation

        val ret_radiances = DoubleArray(11)
        val ret_emission_correction_factor_sun = DoubleArray(11)
        val ret_emission_correction_factor_sky = DoubleArray(11)
        val ret_configs = generateNullArHosekSkyModelConfigurations()

        for (wl in 0 until 11) {
            //   Basic init as for the normal scenario

            ArHosekSkyModel_CookConfiguration(
                    datasets[wl],
                    ret_configs[wl],
                    atmospheric_turbidity,
                    ground_albedo,
                    solar_elevation
            )

            ret_radiances[wl] =
                    ArHosekSkyModel_CookRadianceConfiguration(
                            datasetsRad[wl],
                            atmospheric_turbidity,
                            ground_albedo,
                            solar_elevation
                    )

            //   The wavelength of this band in nanometers

            val owl = (320.0 + 40.0 * wl) * 10E-10

            //   The original intensity we just computed

            val osr = originalSolarRadianceTable[wl]

            //   The intensity of a blackbody with the desired temperature
            //   The fudge factor described above is used to make sure the BB
            //   function matches the used radiance data reasonably well
            //   in magnitude.

            val nsr = art_blackbody_dd_value(solar_surface_temperature_kelvin, owl) * blackbody_scaling_factor

            //   Correction factor for this waveband is simply the ratio of
            //   the two.

            ret_emission_correction_factor_sun[wl] = nsr / osr
        }

        //   We then compute the average correction factor of all wavebands.

        //   Theoretically, some weighting to favour wavelengths human vision is
        //   more sensitive to could be introduced here - think V(lambda). But
        //   given that the whole effort is not *that* accurate to begin with (we
        //   are talking about the appearance of alien worlds, after all), simple
        //   averaging over the visible wavelenghts (! - this is why we start at
        //   WL #2, and only use 2-11) seems like a sane first approximation.

        var correctionFactor = 0.0

        for (i in 2 until 11) {
            correctionFactor += ret_emission_correction_factor_sun[i]
        }

        //   This is the average ratio in emitted energy between our sun, and an
        //   equally large sun with the blackbody spectrum we requested.

        //   Division by 9 because we only used 9 of the 11 wavelengths for this
        //   (see above).

        val ratio = correctionFactor / 9.0

        //   This ratio is then used to determine the radius of the alien sun
        //   on the sky dome. The additional factor 'solar_intensity' can be used
        //   to make the alien sun brighter or dimmer compared to our sun.

        val solar_radius = (sqrt(solar_intensity) * TERRESTRIAL_SOLAR_RADIUS) / sqrt(ratio)

        //   Finally, we have to reduce the scaling factor of the sky by the
        //   ratio used to scale the solar disc size. The rationale behind this is
        //   that the scaling factors apply to the new blackbody spectrum, which
        //   can be more or less bright than the one our sun emits. However, we
        //   just scaled the size of the alien solar disc so it is roughly as
        //   bright (in terms of energy emitted) as the terrestrial sun. So the sky
        //   dome has to be reduced in brightness appropriately - but not in an
        //   uniform fashion across wavebands. If we did that, the sky colour would
        //   be wrong.

        for (i in 0 until 11) {
            ret_emission_correction_factor_sky[i] = solar_intensity * ret_emission_correction_factor_sun[i] / ratio
        }

        return ArHosekSkyModelState(
                ret_configs,
                ret_radiances,
                turbidity,
                solar_radius,
                ret_emission_correction_factor_sky,
                ret_emission_correction_factor_sun,
                albedo, elevation
        )
    }

    fun arhosekskymodelstate_free(state: ArHosekSkyModelState)
    {
        //free(state);
        // only useful for Unsafe implementation
    }

    fun arhosekskymodel_radiance(
            state: ArHosekSkyModelState,
            theta: Double,
            gamma: Double,
            wavelength: Double
    ): Double {
        val low_wl = ((wavelength - 320.0) / 40.0).toInt()

        if (low_wl < 0 || low_wl >= 11) return 0.0

        val interp = fmod((wavelength - 320.0) / 40.0, 1.0)

        val val_low =
        ArHosekSkyModel_GetRadianceInternal(
                state.configs[low_wl],
                theta,
                gamma
        ) * state.radiances[low_wl] * state.emission_correction_factor_sky[low_wl]

        if (interp < 1e-6)
            return val_low

        var result = (1.0 - interp) * val_low

        if (low_wl + 1 < 11) {
            result +=
                    interp * ArHosekSkyModel_GetRadianceInternal(
                            state.configs[low_wl + 1],
                            theta,
                            gamma
                    ) * state.radiances[low_wl + 1] * state.emission_correction_factor_sky[low_wl + 1]
        }

        return result
    }


// xyz and rgb versions

    fun arhosek_xyz_skymodelstate_alloc_init(
            turbidity: Double,
            albedo: Double,
            elevation: Double
    ): ArHosekSkyModelState {
        //ArHosekSkyModelState * state = ALLOC(ArHosekSkyModelState);

        val ret_configs = generateNullArHosekSkyModelConfigurations()
        val ret_radiances = DoubleArray(11)

        for (channel in 0..2) {
            ArHosekSkyModel_CookConfiguration(
                    datasetsXYZ[channel],
                    ret_configs[channel],
                    turbidity,
                    albedo,
                    elevation
            )

            ret_radiances[channel] =
                    ArHosekSkyModel_CookRadianceConfiguration(
                            datasetsXYZRad[channel],
                            turbidity,
                            albedo,
                            elevation
                    )
        }

        return ArHosekSkyModelState(
                ret_configs,
                ret_radiances,
                turbidity,
                TERRESTRIAL_SOLAR_RADIUS,
                DoubleArray(11) { 0.0 },
                DoubleArray(11) { 0.0 },
                albedo, elevation
        )
    }


    fun arhosek_rgb_skymodelstate_alloc_init(
            turbidity: Double,
            albedo: Double,
            elevation: Double
    ): ArHosekSkyModelState {
        //ArHosekSkyModelState * state = ALLOC(ArHosekSkyModelState);

        val ret_configs = generateNullArHosekSkyModelConfigurations()
        val ret_radiances = DoubleArray(11)

        for (channel in 0..2) {
            ArHosekSkyModel_CookConfiguration(
                    datasetsRGB[channel],
                    ret_configs[channel],
                    turbidity,
                    albedo,
                    elevation
            )

            ret_radiances[channel] =
                    ArHosekSkyModel_CookRadianceConfiguration(
                            datasetsRGBRad[channel],
                            turbidity,
                            albedo,
                            elevation
                    )
        }

        return ArHosekSkyModelState(
                ret_configs,
                ret_radiances,
                turbidity,
                TERRESTRIAL_SOLAR_RADIUS,
                DoubleArray(11) { 0.0 },
                DoubleArray(11) { 0.0 },
                albedo, elevation
        )
    }

    fun arhosek_tristim_skymodel_radiance(
            state: ArHosekSkyModelState,
            theta: Double,
            gamma: Double,
            channel: Int
    ): Double {
        return ArHosekSkyModel_GetRadianceInternal(
                state.configs[channel],
                theta,
                gamma
        ) * state.radiances[channel];
    }

    private const val pieces = 45
    private const val order = 4

    fun arhosekskymodel_sr_internal(
            state: ArHosekSkyModelState,
            turbidity: Int,
            wl: Int,
            elevation: Double
    ): Double {
        var pos = (pow(2.0 * elevation / MATH_PI, 1.0 / 3.0) * pieces).toInt() // floor

        if (pos > 44) pos = 44;

        val break_x = pow((pos.toDouble() / pieces.toDouble()), 3.0) * (MATH_PI * 0.5)

        //     const double  * coefs =
        //        solarDatasets[wl] + (order * pieces * turbidity + order * (pos+1) - 1);
        val coefs = DoubleArrayArrayPtr(solarDatasets, wl + (order * pieces * turbidity + order * (pos + 1) - 1))

        var res = 0.0
        val x = elevation - break_x
        var x_exp = 1.0

        for (i in 0 until order) {
            // TODO amirite?
            //res += x_exp * (*(coefs--));
            res += x_exp * coefs.dec().ptrOffset
            x_exp *= x
        }

        return res * state.emission_correction_factor_sun[wl]
    }

    private fun arhosekskymodel_solar_radiance_internal2(
            state: ArHosekSkyModelState,
            wavelength: Double,
            elevation: Double,
            gamma: Double
    ): Double {
        assert(wavelength in 320.0..720.0 && state.turbidity in 1.0..10.0)


        var turb_low = (state.turbidity - 1).toInt()
        var turb_frac = state.turbidity - (turb_low + 1.0)

        if (turb_low == 9) {
            turb_low = 8
            turb_frac = 1.0
        }

        var wl_low = ((wavelength - 320.0) / 40.0).toInt()
        var wl_frac = fmod(wavelength, 40.0) / 40.0

        if (wl_low == 10) {
            wl_low = 9
            wl_frac = 1.0
        }

        var direct_radiance =
                (1.0 - turb_frac) * (
                        (1.0 - wl_frac) * arhosekskymodel_sr_internal(
                        state,
                        turb_low,
                        wl_low,
                        elevation
                ) + wl_frac * arhosekskymodel_sr_internal(
                        state,
                        turb_low,
                        wl_low + 1,
                        elevation
                )
                                    ) + turb_frac* ((1.0 - wl_frac) * arhosekskymodel_sr_internal(
                        state,
                        turb_low + 1,
                        wl_low,
                        elevation
                ) + wl_frac * arhosekskymodel_sr_internal(
                        state,
                        turb_low + 1,
                        wl_low + 1,
                        elevation
                ))


        val ldCoefficient = DoubleArray(6)

        for (i in 0 until 6) {
            ldCoefficient[i] = (1.0 - wl_frac) * limbDarkeningDatasets[wl_low][i] +
                               wl_frac * limbDarkeningDatasets[wl_low + 1][i]
        }

        // sun distance to diameter ratio, squared

        val sol_rad_sin = sin(state.solar_radius)
        val ar2 = 1.0 / (sol_rad_sin * sol_rad_sin)
        val singamma = sin(gamma)
        var sc2 = 1.0 - ar2 * singamma * singamma
        if (sc2 < 0.0) sc2 = 0.0
        var sampleCosine = sqrt (sc2);

        //   The following will be improved in future versions of the model:
        //   here, we directly use fitted 5th order polynomials provided by the
        //   astronomical community for the limb darkening effect. Astronomers need
        //   such accurate fittings for their predictions. However, this sort of
        //   accuracy is not really needed for CG purposes, so an approximated
        //   dataset based on quadratic polynomials will be provided in a future
        //   release.

        var darkeningFactor =
                ldCoefficient[0] +
                ldCoefficient[1] * sampleCosine +
                ldCoefficient[2] * pow(sampleCosine, 2.0) +
                ldCoefficient[3] * pow(sampleCosine, 3.0) +
                ldCoefficient[4] * pow(sampleCosine, 4.0) +
                ldCoefficient[5] * pow(sampleCosine, 5.0)

        direct_radiance *= darkeningFactor;

        return direct_radiance;
    }

    fun arhosekskymodel_solar_radiance(
            state: ArHosekSkyModelState,
            theta: Double,
            gamma: Double,
            wavelength: Double
    ): Double {
        val direct_radiance = arhosekskymodel_solar_radiance_internal2(
                state,
                wavelength,
                ((MATH_PI / 2.0) - theta),
                gamma
        )

        val inscattered_radiance = arhosekskymodel_radiance(
                state,
                theta,
                gamma,
                wavelength
        )

        return direct_radiance + inscattered_radiance;
    }
}

private val notes_from_author = """
/*
This source is published under the following 3-clause BSD license.

Copyright (c) 2012 - 2013, Lukas Hosek and Alexander Wilkie
All rights reserved.

Redistribution and use in source and binary forms, with or without 
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * None of the names of the contributors may be used to endorse or promote 
      products derived from this software without specific prior written 
      permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/


/* ============================================================================

This file is part of a sample implementation of the analytical skylight and
solar radiance models presented in the SIGGRAPH 2012 paper


           "An Analytic Model for Full Spectral Sky-Dome Radiance"

and the 2013 IEEE CG&A paper

       "Adding a Solar Radiance Function to the Hosek Skylight Model"

                                   both by 

                       Lukas Hosek and Alexander Wilkie
                Charles University in Prague, Czech Republic


                        Version: 1.4a, February 22nd, 2013
                        
Version history:

1.4a  February 22nd, 2013
      Removed unnecessary and counter-intuitive solar radius parameters 
      from the interface of the colourspace sky dome initialisation functions.

1.4   February 11th, 2013
      Fixed a bug which caused the relative brightness of the solar disc
      and the sky dome to be off by a factor of about 6. The sun was too 
      bright: this affected both normal and alien sun scenarios. The 
      coefficients of the solar radiance function were changed to fix this.

1.3   January 21st, 2013 (not released to the public)
      Added support for solar discs that are not exactly the same size as
      the terrestrial sun. Also added support for suns with a different
      emission spectrum ("Alien World" functionality).

1.2a  December 18th, 2012
      Fixed a mistake and some inaccuracies in the solar radiance function
      explanations found in ArHosekSkyModel.h. The actual source code is
      unchanged compared to version 1.2.

1.2   December 17th, 2012
      Native RGB data and a solar radiance function that matches the turbidity
      conditions were added.

1.1   September 2012
      The coefficients of the spectral model are now scaled so that the output
      is given in physical units: W / (m^-2 * sr * nm). Also, the output of the
      XYZ model is now no longer scaled to the range [0...1]. Instead, it is
      the result of a simple conversion from spectral data via the CIE 2 degree
      standard observer matching functions. Therefore, after multiplication
      with 683 lm / W, the Y channel now corresponds to luminance in lm.
     
1.0   May 11th, 2012
      Initial release.


Please visit http://cgg.mff.cuni.cz/projects/SkylightModelling/ to check if
an updated version of this code has been published!

============================================================================ */


/*

This code is taken from ART, a rendering research system written in a
mix of C99 / Objective C. Since ART is not a small system and is intended to 
be inter-operable with other libraries, and since C does not have namespaces, 
the structures and functions in ART all have to have somewhat wordy 
canonical names that begin with Ar.../ar..., like those seen in this example.

Usage information:
==================


Model initialisation
--------------------

A separate ArHosekSkyModelState has to be maintained for each spectral
band you want to use the model for. So in a renderer with 'num_channels'
bands, you would need something like

    ArHosekSkyModelState  * skymodel_state[num_channels];

You then have to allocate and initialise these states. In the following code
snippet, we assume that 'albedo' is defined as

    double  albedo[num_channels];

with a ground albedo value between [0,1] for each channel. The solar elevation  
is given in radians.

    for ( unsigned int i = 0; i < num_channels; i++ )
        skymodel_state[i] =
            arhosekskymodelstate_alloc_init(
                  turbidity,
                  albedo[i],
                  solarElevation
                );

Note that starting with version 1.3, there is also a second initialisation 
function which generates skydome states for different solar emission spectra 
and solar radii: 'arhosekskymodelstate_alienworld_alloc_init()'.

See the notes about the "Alien World" functionality provided further down for a 
discussion of the usefulness and limits of that second initalisation function.
Sky model states that have been initialised with either function behave in a
completely identical fashion during use and cleanup.

Using the model to generate skydome samples
-------------------------------------------

Generating a skydome radiance spectrum "skydome_result" for a given location
on the skydome determined via the angles theta and gamma works as follows:

    double  skydome_result[num_channels];

    for ( unsigned int i = 0; i < num_channels; i++ )
        skydome_result[i] =
            arhosekskymodel_radiance(
                skymodel_state[i],
                theta,
                gamma,
                channel_center[i]
              );
              
The variable "channel_center" is assumed to hold the channel center wavelengths
for each of the num_channels samples of the spectrum we are building.


Cleanup after use
-----------------

After rendering is complete, the content of the sky model states should be
disposed of via

        for ( unsigned int i = 0; i < num_channels; i++ )
            arhosekskymodelstate_free( skymodel_state[i] );


CIE XYZ Version of the Model
----------------------------

Usage of the CIE XYZ version of the model is exactly the same, except that
num_channels is of course always 3, and that ArHosekTristimSkyModelState and
arhosek_tristim_skymodel_radiance() have to be used instead of their spectral
counterparts.

RGB Version of the Model
------------------------

The RGB version uses sRGB primaries with a linear gamma ramp. The same set of
functions as with the XYZ data is used, except the model is initialized
by calling arhosek_rgb_skymodelstate_alloc_init.

Solar Radiance Function
-----------------------

For each position on the solar disc, this function returns the entire radiance 
one sees - direct emission, as well as in-scattered light in the area of the 
solar disc. The latter is important for low solar elevations - nice images of 
the setting sun would not be possible without this. This is also the reason why 
this function, just like the regular sky dome model evaluation function, needs 
access to the sky dome data structures, as these provide information on 
in-scattered radiance.

CAVEAT #1: in this release, this function is only provided in spectral form!
           RGB/XYZ versions to follow at a later date.

CAVEAT #2: (fixed from release 1.3 onwards) 

CAVEAT #3: limb darkening renders the brightness of the solar disc
           inhomogeneous even for high solar elevations - only taking a single
           sample at the centre of the sun will yield an incorrect power
           estimate for the solar disc! Always take multiple random samples
           across the entire solar disc to estimate its power!
           
CAVEAT #4: in this version, the limb darkening calculations still use a fairly
           computationally expensive 5th order polynomial that was directly 
           taken from astronomical literature. For the purposes of Computer
           Graphics, this is needlessly accurate, though, and will be replaced 
           by a cheaper approximation in a future release.

"Alien World" functionality
---------------------------

The Hosek sky model can be used to roughly (!) predict the appearance of 
outdoor scenes on earth-like planets, i.e. planets of a similar size and 
atmospheric make-up. Since the spectral version of our model predicts sky dome 
luminance patterns and solar radiance independently for each waveband, and 
since the intensity of each waveband is solely dependent on the input radiance 
from the star that the world in question is orbiting, it is trivial to re-scale 
the wavebands to match a different star radiance.

At least in theory, the spectral version of the model has always been capable 
of this sort of thing, and the actual sky dome and solar radiance models were 
actually not altered at all in this release. All we did was to add some support
functionality for doing this more easily with the existing data and functions, 
and to add some explanations.

Just use 'arhosekskymodelstate_alienworld_alloc_init()' to initialise the sky
model states (you will have to provide values for star temperature and solar 
intensity compared to the terrestrial sun), and do everything else as you 
did before.

CAVEAT #1: we assume the emission of the star that illuminates the alien world 
           to be a perfect blackbody emission spectrum. This is never entirely 
           realistic - real star emission spectra are considerably more complex 
           than this, mainly due to absorption effects in the outer layers of 
           stars. However, blackbody spectra are a reasonable first assumption 
           in a usage scenario like this, where 100% accuracy is simply not 
           necessary: for rendering purposes, there are likely no visible 
           differences between a highly accurate solution based on a more 
           involved simulation, and this approximation.

CAVEAT #2: we always use limb darkening data from our own sun to provide this
           "appearance feature", even for suns of strongly different 
           temperature. Which is presumably not very realistic, but (as with 
           the unaltered blackbody spectrum from caveat #1) probably not a bad 
           first guess, either. If you need more accuracy than we provide here,
           please make inquiries with a friendly astro-physicst of your choice.

CAVEAT #3: you have to provide a value for the solar intensity of the star 
           which illuminates the alien world. For this, please bear in mind  
           that there is very likely a comparatively tight range of absolute  
           solar irradiance values for which an earth-like planet with an  
           atmosphere like the one we assume in our model can exist in the  
           first place!
            
           Too much irradiance, and the atmosphere probably boils off into 
           space, too little, it freezes. Which means that stars of 
           considerably different emission colour than our sun will have to be 
           fairly different in size from it, to still provide a reasonable and 
           inhabitable amount of irradiance. Red stars will need to be much 
           larger than our sun, while white or blue stars will have to be 
           comparatively tiny. The initialisation function handles this and 
           computes a plausible solar radius for a given emission spectrum. In
           terms of absolute radiometric values, you should probably not stray
           all too far from a solar intensity value of 1.0.

CAVEAT #4: although we now support different solar radii for the actual solar 
           disc, the sky dome luminance patterns are *not* parameterised by 
           this value - i.e. the patterns stay exactly the same for different 
           solar radii! Which is of course not correct. But in our experience, 
           solar discs up to several degrees in diameter (! - our own sun is 
           half a degree across) do not cause the luminance patterns on the sky 
           to change perceptibly. The reason we know this is that we initially 
           used unrealistically large suns in our brute force path tracer, in 
           order to improve convergence speeds (which in the beginning were 
           abysmal). Later, we managed to do the reference renderings much 
           faster even with realistically small suns, and found that there was 
           no real difference in skydome appearance anyway. 
           Conclusion: changing the solar radius should not be over-done, so  
           close orbits around red supergiants are a no-no. But for the  
           purposes of getting a fairly credible first impression of what an 
           alien world with a reasonably sized sun would look like, what we are  
           doing here is probably still o.k.

HINT #1:   if you want to model the sky of an earth-like planet that orbits 
           a binary star, just super-impose two of these models with solar 
           intensity of ~0.5 each, and closely spaced solar positions. Light is
           additive, after all. Tattooine, here we come... :-)

           P.S. according to Star Wars canon, Tattooine orbits a binary
           that is made up of a G and K class star, respectively. 
           So ~5500K and ~4200K should be good first guesses for their 
           temperature. Just in case you were wondering, after reading the
           previous paragraph.
*/


#ifndef _ARHOSEK_SKYMODEL_H_
#define _ARHOSEK_SKYMODEL_H_

typedef double ArHosekSkyModelConfiguration[9];


//   Spectral version of the model

/* ----------------------------------------------------------------------------

    ArHosekSkyModelState struct
    ---------------------------

    This struct holds the pre-computation data for one particular albedo value.
    Most fields are self-explanatory, but users should never directly 
    manipulate any of them anyway. The only consistent way to manipulate such 
    structs is via the functions 'arhosekskymodelstate_alloc_init' and 
    'arhosekskymodelstate_free'.
    
    'emission_correction_factor_sky'
    'emission_correction_factor_sun'

        The original model coefficients were fitted against the emission of 
        our local sun. If a different solar emission is desired (i.e. if the
        model is being used to predict skydome appearance for an earth-like 
        planet that orbits a different star), these correction factors, which 
        are determined during the alloc_init step, are applied to each waveband 
        separately (they default to 1.0 in normal usage). This is the simplest 
        way to retrofit this sort of capability to the existing model. The 
        different factors for sky and sun are needed since the solar disc may 
        be of a different size compared to the terrestrial sun.

---------------------------------------------------------------------------- */

typedef struct ArHosekSkyModelState
{
    ArHosekSkyModelConfiguration  configs[11];
    double                        radiances[11];
    double                        turbidity;
    double                        solar_radius;
    double                        emission_correction_factor_sky[11];
    double                        emission_correction_factor_sun[11];
    double                        albedo;
    double                        elevation;
} 
ArHosekSkyModelState;

/* ----------------------------------------------------------------------------

    arhosekskymodelstate_alloc_init() function
    ------------------------------------------

    Initialises an ArHosekSkyModelState struct for a terrestrial setting.

---------------------------------------------------------------------------- */

ArHosekSkyModelState  * arhosekskymodelstate_alloc_init(
        const double  solar_elevation,
        const double  atmospheric_turbidity,
        const double  ground_albedo
        );


/* ----------------------------------------------------------------------------

    arhosekskymodelstate_alienworld_alloc_init() function
    -----------------------------------------------------

    Initialises an ArHosekSkyModelState struct for an "alien world" setting
    with a sun of a surface temperature given in 'kelvin'. The parameter
    'solar_intensity' controls the overall brightness of the sky, relative
    to the solar irradiance on Earth. A value of 1.0 yields a sky dome that
    is, on average over the wavelenghts covered in the model (!), as bright
    as the terrestrial sky in radiometric terms. 
    
    Which means that the solar radius has to be adjusted, since the 
    emissivity of a solar surface with a given temperature is more or less 
    fixed. So hotter suns have to be smaller to be equally bright as the 
    terrestrial sun, while cooler suns have to be larger. Note that there are
    limits to the validity of the luminance patterns of the underlying model:
    see the discussion above for more on this. In particular, an alien sun with
    a surface temperature of only 2000 Kelvin has to be very large if it is
    to be as bright as the terrestrial sun - so large that the luminance 
    patterns are no longer a really good fit in that case.
    
    If you need information about the solar radius that the model computes
    for a given temperature (say, for light source sampling purposes), you 
    have to query the 'solar_radius' variable of the sky model state returned 
    *after* running this function.

---------------------------------------------------------------------------- */

ArHosekSkyModelState  * arhosekskymodelstate_alienworld_alloc_init(
        const double  solar_elevation,
        const double  solar_intensity,
        const double  solar_surface_temperature_kelvin,
        const double  atmospheric_turbidity,
        const double  ground_albedo
        );

void arhosekskymodelstate_free(
        ArHosekSkyModelState  * state
        );

double arhosekskymodel_radiance(
        ArHosekSkyModelState  * state,
        double                  theta, 
        double                  gamma, 
        double                  wavelength
        );

// CIE XYZ and RGB versions


ArHosekSkyModelState  * arhosek_xyz_skymodelstate_alloc_init(
        const double  turbidity, 
        const double  albedo, 
        const double  elevation
        );


ArHosekSkyModelState  * arhosek_rgb_skymodelstate_alloc_init(
        const double  turbidity, 
        const double  albedo, 
        const double  elevation
        );


double arhosek_tristim_skymodel_radiance(
        ArHosekSkyModelState  * state,
        double                  theta,
        double                  gamma, 
        int                     channel
        );

//   Delivers the complete function: sky + sun, including limb darkening.
//   Please read the above description before using this - there are several
//   caveats!

double arhosekskymodel_solar_radiance(
        ArHosekSkyModelState      * state,
        double                      theta,
        double                      gamma,
        double                      wavelength
        );


#endif // _ARHOSEK_SKYMODEL_H_

"""
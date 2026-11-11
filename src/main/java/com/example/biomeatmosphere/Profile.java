package com.example.biomeatmosphere;

import org.jetbrains.annotations.Nullable;

/**
 * Атмосфера одного типа биома.
 *
 * @param fogDensity    множитель дальности тумана (1 = ваниль, меньше = гуще)
 * @param mist          насколько биом подвержен утренней и вечерней дымке (0..1)
 * @param skyTint       множитель цвета неба и горизонта (0xFFFFFF = без изменений)
 * @param waterTint     множитель цвета воды
 * @param gradeColor    цвет лёгкого оттенка экрана
 * @param gradeAlpha    прозрачность оттенка (0.02..0.06 заметно, но мягко)
 * @param dayParticles  частицы днём
 * @param nightParticles частицы ночью
 * @param particleRate  множитель количества частиц
 */
public record Profile(
        String id,
        float fogDensity,
        float mist,
        int skyTint,
        int waterTint,
        int gradeColor,
        float gradeAlpha,
        @Nullable MoteKind dayParticles,
        @Nullable MoteKind nightParticles,
        float particleRate) {
}

package com.eu.habbo.habbohotel.habboway;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Pure rules of the official "Habbo Way" quiz (AIR HabboWayQuizController):
 * the server hands out {@link #QUESTIONS_PER_QUIZ} of the ten
 * {@code quiz.HabboWay1.question.<id>} questions, the client answers them in
 * that order with the index of the chosen {@code quiz.HabboWay1.answer.<id>.<n>}
 * text, and the server replies with the ids of the wrongly answered questions.
 *
 * <p>The answer key mirrors the static fallback of the client
 * ({@code HABBO_WAY_STATIC_ANSWER_KEY} in {@code ui/src/hooks/help/habboWayQuiz.ts})
 * so both paths grade identically.
 */
public final class HabboWayQuiz {

    public static final String QUIZ_CODE = "HabboWay1";
    public static final int QUESTIONS_PER_QUIZ = 5;

    /** Correct answer index per question id 0..9 of the official texts. */
    private static final int[] ANSWER_KEY = {2, 1, 2, 0, 1, 3, 1, 3, 0, 0};

    private HabboWayQuiz() {}

    public static boolean isKnownQuiz(String quizCode) {
        return QUIZ_CODE.equals(quizCode);
    }

    public static int questionCount() {
        return ANSWER_KEY.length;
    }

    public static int correctAnswer(int questionId) {
        if (questionId < 0 || questionId >= ANSWER_KEY.length) {
            return -1;
        }
        return ANSWER_KEY[questionId];
    }

    /** Draws {@link #QUESTIONS_PER_QUIZ} distinct question ids in random order. */
    public static int[] pickQuestions(Random random) {
        List<Integer> pool = new ArrayList<>();
        for (int id = 0; id < ANSWER_KEY.length; id++) {
            pool.add(id);
        }
        int[] picked = new int[Math.min(QUESTIONS_PER_QUIZ, pool.size())];
        for (int i = 0; i < picked.length; i++) {
            picked[i] = pool.remove(random.nextInt(pool.size()));
        }
        return picked;
    }

    /**
     * Grades the answers given in question order; a missing (-1) or out of
     * range answer counts as wrong. Returns the wrongly answered question ids
     * in question order.
     */
    public static List<Integer> grade(int[] questionIds, int[] answerIds) {
        List<Integer> wrong = new ArrayList<>();
        for (int i = 0; i < questionIds.length; i++) {
            int answer = i < answerIds.length ? answerIds[i] : -1;
            if (correctAnswer(questionIds[i]) != answer) {
                wrong.add(questionIds[i]);
            }
        }
        return wrong;
    }

    public static boolean isPerfect(int[] questionIds, int[] answerIds) {
        return questionIds.length > 0 && grade(questionIds, answerIds).isEmpty();
    }

    static boolean isValidQuestionSet(int[] questionIds) {
        if (questionIds == null || questionIds.length != QUESTIONS_PER_QUIZ) {
            return false;
        }
        int[] sorted = questionIds.clone();
        Arrays.sort(sorted);
        for (int i = 0; i < sorted.length; i++) {
            if (sorted[i] < 0 || sorted[i] >= ANSWER_KEY.length || (i > 0 && sorted[i] == sorted[i - 1])) {
                return false;
            }
        }
        return true;
    }
}

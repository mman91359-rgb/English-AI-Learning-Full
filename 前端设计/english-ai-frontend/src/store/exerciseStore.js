import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useExerciseStore = defineStore('exercise', () => {
  const currentQuestions = ref([])
  const currentIndex = ref(0)
  const answers = ref({})
  const moduleType = ref(1)
  const mode = ref('practice') // 'assess' | 'practice' | 'review'

  const currentQuestion = computed(() => currentQuestions.value[currentIndex.value] || null)
  const progress = computed(() => currentQuestions.value.length ? (currentIndex.value / currentQuestions.value.length) * 100 : 0)
  const totalCount = computed(() => currentQuestions.value.length)
  const answeredCount = computed(() => Object.keys(answers.value).length)

  function startExercise(questions, type, m = 'practice') {
    currentQuestions.value = questions
    currentIndex.value = 0
    answers.value = {}
    moduleType.value = type
    mode.value = m
  }

  function submitAnswer(questionId, result) {
    answers.value[questionId] = result
  }

  function nextQuestion() {
    if (currentIndex.value < currentQuestions.value.length - 1) {
      currentIndex.value++
    }
  }

  function prevQuestion() {
    if (currentIndex.value > 0) {
      currentIndex.value--
    }
  }

  function reset() {
    currentQuestions.value = []
    currentIndex.value = 0
    answers.value = {}
    mode.value = 'practice'
  }

  return { currentQuestions, currentIndex, answers, moduleType, mode, currentQuestion, progress, totalCount, answeredCount, startExercise, submitAnswer, nextQuestion, prevQuestion, reset }
})

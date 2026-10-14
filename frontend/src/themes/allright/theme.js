import { CHARACTER_EVENTS } from '../characterEvents'
import normal from './assets/alright-normal.webp'
import fighting from './assets/alright-fighting.webp'
import thumbsUp from './assets/alright-thumbs-up.webp'
import oneMore from './assets/alright-one-more.webp'
import checkRecord from './assets/alright-check-record.webp'
import windmill from './assets/alright-windmill.webp'
import wakeUp from './assets/alright-wake-up.webp'
import messages from './messages'

export default {
  id: 'allright',
  images: {
    [CHARACTER_EVENTS.DASHBOARD_ENTER]: normal,
    [CHARACTER_EVENTS.WORKOUT_ENTER]: fighting,
    [CHARACTER_EVENTS.ROUTINE_START]: fighting,
    [CHARACTER_EVENTS.ROUTINE_SELECT]: fighting,
    [CHARACTER_EVENTS.FREE_WORKOUT_START]: fighting,
    [CHARACTER_EVENTS.EXERCISE_ADD]: thumbsUp,
    [CHARACTER_EVENTS.SET_COMPLETE]: thumbsUp,
    [CHARACTER_EVENTS.SET_ADD]: oneMore,
    [CHARACTER_EVENTS.RECORD_CHECK]: checkRecord,
    [CHARACTER_EVENTS.RECORD_LOAD]: checkRecord,
    [CHARACTER_EVENTS.WORKOUT_RECORD_OPEN]: checkRecord,
    [CHARACTER_EVENTS.WORKOUT_SAVE_SUCCESS]: windmill,
    [CHARACTER_EVENTS.WORKOUT_SAVE_ERROR]: wakeUp,
  },
  messages,
}

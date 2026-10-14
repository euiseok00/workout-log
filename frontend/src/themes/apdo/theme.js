import { CHARACTER_EVENTS } from '../characterEvents'
import normal from './assets/apdo-normal.webp'
import fighting from './assets/apdo-fighting.webp'
import thumbsUp from './assets/apdo-thumbs-up.webp'
import oneMore from './assets/apdo-one-more.webp'
import checkRecord from './assets/apdo-check-record.webp'
import success from './assets/apdo-success.webp'
import fail from './assets/apdo-fail.webp'
import messages from './messages'

export default {
  id: 'apdo',
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
    [CHARACTER_EVENTS.WORKOUT_SAVE_SUCCESS]: success,
    [CHARACTER_EVENTS.WORKOUT_SAVE_ERROR]: fail,
  },
  messages,
}

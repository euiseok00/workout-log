import { api } from '../../lib/api'
import { ConfirmModal } from '../../components/ui'

export default function DeleteRoutineModal({ routine, onClose, onDeleted }) {
  const remove = async () => {
    await api(`/api/routines/${routine.id}`, { method: 'DELETE' })
    onDeleted()
  }
  return <ConfirmModal title="루틴 삭제" message={`“${routine.name}” 루틴을 삭제하시겠습니까? 완료한 운동 기록은 유지됩니다.`} onConfirm={remove} onClose={onClose}/>
}

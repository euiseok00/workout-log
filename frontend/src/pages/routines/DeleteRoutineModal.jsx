import { useState } from 'react'
import { api } from '../../lib/api'
import { ErrorMessage, Modal } from '../../components/ui'

export default function DeleteRoutineModal({ routine, onClose, onDeleted }) {
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState(null)
  const remove = async () => {
    setDeleting(true); setError(null)
    try {
      await api(`/api/routines/${routine.id}`, { method: 'DELETE' })
      onDeleted()
    } catch (requestError) { setError(requestError); setDeleting(false) }
  }
  return <Modal title="루틴 삭제" onClose={() => !deleting && onClose()}><p>“{routine.name}” 루틴을 삭제하시겠습니까? 완료한 운동 기록은 유지됩니다.</p><ErrorMessage error={error}/><div className="detail-delete-actions"><button type="button" className="secondary-button" onClick={onClose} disabled={deleting}>취소</button><button type="button" className="primary-button delete-confirm-button" onClick={remove} disabled={deleting}>{deleting ? '삭제 중...' : '삭제'}</button></div></Modal>
}

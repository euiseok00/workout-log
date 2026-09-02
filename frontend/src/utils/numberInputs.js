export function isDbWeightInput(value) {
  return /^\d{0,3}(\.\d{0,2})?$/.test(value)
}

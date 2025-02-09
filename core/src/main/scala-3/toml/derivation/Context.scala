package toml
package derivation

/** This type represents derivation context.
  *
  * Codec derivation is stateful operation in that
  *   - it removes an entry from TOML table when it successfully parses a field
  *   - it reads TOML array by mutating index
  *   - it accumulates error address as a list
  *
  * @param tomlValue
  *   the TOML value from which the value of type `P` is built
  * @param labels
  *   field names of type `P`
  * @param index
  *   index of array elements
  * @param error
  *   a pair of error address and error message.
  */
private final case class Context(
    tomlValue: Value,
    labels: Seq[String],
    index: Int,
    error: Parse.Error,
):
  def head = labels.head
  def tail = labels.tail
  def errorAddress = error._1
  def errorMessage = error._2

  def withErrorMessage(message: String) = copy(
    error = (errorAddress, message),
  )
  def withError(address: List[String], message: String) = copy(
    error = (address, message),
  )

  /** It removes the first label from the list and continue with remaining
    * labels
    */
  def continueWithTail = copy(
    tomlValue,
    tail,
    index,
    error,
  )
end Context

private object Context:
  def apply(
      tomlValue: Value,
      labels: Seq[String],
      index: Int,
  ) = new Context(tomlValue, labels, index, (Nil, ""))

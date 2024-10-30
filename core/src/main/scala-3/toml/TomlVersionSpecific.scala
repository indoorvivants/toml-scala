package toml

private[toml] trait TomlVersionSpecific {

    def parseAs[A](value: Value)(using codec: Codec[A]): Either[Parse.Error, A] =
      codec(value, Map(), 0)

    def parseAs[A](toml: String, extensions: Set[Extension] = Set())(using
        codec: Codec[A]
    ): Either[Parse.Error, A] =
      Toml.parse(toml, extensions).right.flatMap(codec(_, Map(), 0))
  
    def parseAsValue[A](value: Value)(using codec: Codec[A]): Either[Parse.Error, A] =
      codec(value, Map(), 0)

    def parseAsValue[A](toml: String, extensions: Set[Extension] = Set())(using
        codec: Codec[A]
    ): Either[Parse.Error, A] =
      Toml.parse(toml, extensions).right.flatMap(codec(_, Map(), 0))
  
}


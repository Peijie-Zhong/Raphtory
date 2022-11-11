from pyraphtory.interop import ScalaClassProxy


class GraphBuilder(ScalaClassProxy):
    _classname = "com.raphtory.api.input.GraphBuilder"


class Type(ScalaClassProxy):
    _classname = "com.raphtory.api.input.Type"


class StringProperty(ScalaClassProxy):
    _classname = "com.raphtory.api.input.StringProperty"


class ImmutableProperty(ScalaClassProxy):
    _classname = "com.raphtory.api.input.ImmutableProperty"


class Properties(ScalaClassProxy):
    _classname = "com.raphtory.api.input.Properties"


class Source(ScalaClassProxy):
    _classname = "com.raphtory.api.input.Source"

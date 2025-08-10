package com.zhufucdev.practiso.datamodel

import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.serializers.FormattedInstantSerializer

object InstantIsoSerializer : FormattedInstantSerializer(
    "com.zhufucdev.practiso.InstantIsoSerializer",
    DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET
)
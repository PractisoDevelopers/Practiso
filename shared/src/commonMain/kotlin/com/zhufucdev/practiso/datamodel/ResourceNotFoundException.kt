package com.zhufucdev.practiso.service

class ResourceNotFoundException(val resourceName: String) :
    Exception("Resource \"${resourceName}\" is absent.")
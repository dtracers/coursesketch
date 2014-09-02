# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: input/submission.proto

from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import descriptor_pb2
# @@protoc_insertion_point(imports)




DESCRIPTOR = _descriptor.FileDescriptor(
  name='input/submission.proto',
  package='protobuf.srl.submission',
  serialized_pb='\n\x16input/submission.proto\x12\x17protobuf.srl.submission\"\x8f\x01\n\rSrlSubmission\x12\n\n\x02id\x18\x01 \x01(\t\x12\x12\n\nupdateList\x18\x02 \x01(\x0c\x12\x0e\n\x06sketch\x18\x03 \x01(\x0c\x12\x16\n\x0esubmissionTime\x18\x04 \x01(\x03\x12\x36\n\x08\x63hecksum\x18\x05 \x01(\x0b\x32$.protobuf.srl.submission.SrlChecksum\"\x99\x01\n\x0bSrlSolution\x12\x1c\n\x14\x61llowedInProblemBank\x18\x01 \x01(\x08\x12\x19\n\x11isPracticeProblem\x18\x02 \x01(\x08\x12:\n\nsubmission\x18\x03 \x01(\x0b\x32&.protobuf.srl.submission.SrlSubmission\x12\x15\n\rproblemBankId\x18\x04 \x01(\t\"\x96\x01\n\rSrlExperiment\x12\x10\n\x08\x63ourseId\x18\x01 \x01(\t\x12\x14\n\x0c\x61ssignmentId\x18\x02 \x01(\t\x12\x11\n\tproblemId\x18\x03 \x01(\t\x12\x0e\n\x06userId\x18\x05 \x01(\t\x12:\n\nsubmission\x18\x06 \x01(\x0b\x32&.protobuf.srl.submission.SrlSubmission\"4\n\x0bSrlChecksum\x12\x11\n\tfirstBits\x18\x01 \x02(\x03\x12\x12\n\nsecondBits\x18\x02 \x02(\x03\"P\n\x11SrlExperimentList\x12;\n\x0b\x65xperiments\x18\x01 \x03(\x0b\x32&.protobuf.srl.submission.SrlExperiment')




_SRLSUBMISSION = _descriptor.Descriptor(
  name='SrlSubmission',
  full_name='protobuf.srl.submission.SrlSubmission',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='id', full_name='protobuf.srl.submission.SrlSubmission.id', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=unicode("", "utf-8"),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='updateList', full_name='protobuf.srl.submission.SrlSubmission.updateList', index=1,
      number=2, type=12, cpp_type=9, label=1,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='sketch', full_name='protobuf.srl.submission.SrlSubmission.sketch', index=2,
      number=3, type=12, cpp_type=9, label=1,
      has_default_value=False, default_value="",
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='submissionTime', full_name='protobuf.srl.submission.SrlSubmission.submissionTime', index=3,
      number=4, type=3, cpp_type=2, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='checksum', full_name='protobuf.srl.submission.SrlSubmission.checksum', index=4,
      number=5, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=52,
  serialized_end=195,
)


_SRLSOLUTION = _descriptor.Descriptor(
  name='SrlSolution',
  full_name='protobuf.srl.submission.SrlSolution',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='allowedInProblemBank', full_name='protobuf.srl.submission.SrlSolution.allowedInProblemBank', index=0,
      number=1, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='isPracticeProblem', full_name='protobuf.srl.submission.SrlSolution.isPracticeProblem', index=1,
      number=2, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='submission', full_name='protobuf.srl.submission.SrlSolution.submission', index=2,
      number=3, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='problemBankId', full_name='protobuf.srl.submission.SrlSolution.problemBankId', index=3,
      number=4, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=unicode("", "utf-8"),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=198,
  serialized_end=351,
)


_SRLEXPERIMENT = _descriptor.Descriptor(
  name='SrlExperiment',
  full_name='protobuf.srl.submission.SrlExperiment',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='courseId', full_name='protobuf.srl.submission.SrlExperiment.courseId', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=unicode("", "utf-8"),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='assignmentId', full_name='protobuf.srl.submission.SrlExperiment.assignmentId', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=unicode("", "utf-8"),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='problemId', full_name='protobuf.srl.submission.SrlExperiment.problemId', index=2,
      number=3, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=unicode("", "utf-8"),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='userId', full_name='protobuf.srl.submission.SrlExperiment.userId', index=3,
      number=5, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=unicode("", "utf-8"),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='submission', full_name='protobuf.srl.submission.SrlExperiment.submission', index=4,
      number=6, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=354,
  serialized_end=504,
)


_SRLCHECKSUM = _descriptor.Descriptor(
  name='SrlChecksum',
  full_name='protobuf.srl.submission.SrlChecksum',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='firstBits', full_name='protobuf.srl.submission.SrlChecksum.firstBits', index=0,
      number=1, type=3, cpp_type=2, label=2,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
    _descriptor.FieldDescriptor(
      name='secondBits', full_name='protobuf.srl.submission.SrlChecksum.secondBits', index=1,
      number=2, type=3, cpp_type=2, label=2,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=506,
  serialized_end=558,
)


_SRLEXPERIMENTLIST = _descriptor.Descriptor(
  name='SrlExperimentList',
  full_name='protobuf.srl.submission.SrlExperimentList',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='experiments', full_name='protobuf.srl.submission.SrlExperimentList.experiments', index=0,
      number=1, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      options=None),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  options=None,
  is_extendable=False,
  extension_ranges=[],
  serialized_start=560,
  serialized_end=640,
)

_SRLSUBMISSION.fields_by_name['checksum'].message_type = _SRLCHECKSUM
_SRLSOLUTION.fields_by_name['submission'].message_type = _SRLSUBMISSION
_SRLEXPERIMENT.fields_by_name['submission'].message_type = _SRLSUBMISSION
_SRLEXPERIMENTLIST.fields_by_name['experiments'].message_type = _SRLEXPERIMENT
DESCRIPTOR.message_types_by_name['SrlSubmission'] = _SRLSUBMISSION
DESCRIPTOR.message_types_by_name['SrlSolution'] = _SRLSOLUTION
DESCRIPTOR.message_types_by_name['SrlExperiment'] = _SRLEXPERIMENT
DESCRIPTOR.message_types_by_name['SrlChecksum'] = _SRLCHECKSUM
DESCRIPTOR.message_types_by_name['SrlExperimentList'] = _SRLEXPERIMENTLIST

class SrlSubmission(_message.Message):
  __metaclass__ = _reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _SRLSUBMISSION

  # @@protoc_insertion_point(class_scope:protobuf.srl.submission.SrlSubmission)

class SrlSolution(_message.Message):
  __metaclass__ = _reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _SRLSOLUTION

  # @@protoc_insertion_point(class_scope:protobuf.srl.submission.SrlSolution)

class SrlExperiment(_message.Message):
  __metaclass__ = _reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _SRLEXPERIMENT

  # @@protoc_insertion_point(class_scope:protobuf.srl.submission.SrlExperiment)

class SrlChecksum(_message.Message):
  __metaclass__ = _reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _SRLCHECKSUM

  # @@protoc_insertion_point(class_scope:protobuf.srl.submission.SrlChecksum)

class SrlExperimentList(_message.Message):
  __metaclass__ = _reflection.GeneratedProtocolMessageType
  DESCRIPTOR = _SRLEXPERIMENTLIST

  # @@protoc_insertion_point(class_scope:protobuf.srl.submission.SrlExperimentList)


# @@protoc_insertion_point(module_scope)

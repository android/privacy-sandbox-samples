# In App Mediatee Adapter SDK

This SDK is Runtime Aware but runs in the App process. It facilitates interaction between mediator
and in-app mediatee.

Implements MediateeAdapterInterface declared by mediator (runtime-enabled).

This could be owned by the mediator sdk during transition, or optionally all the logic here could
also be a part of RA_SDK (runtime-aware-sdk).

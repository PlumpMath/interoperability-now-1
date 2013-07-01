#tag ClassProtected Class XLIFFdocError	#tag Method, Flags = &h0		Sub Constructor(theErrorType as String)		  me.errorType = theErrorType		  		  me.warningLevelMessage = array("Error","Warning")		  		End Sub	#tag EndMethod	#tag Method, Flags = &h0		Sub Constructor(theErrorType as String, theMessage as String)		  me.errorType = theErrorType		  me.message = theMessage		  		  me.warningLevelMessage = array("Error","Warning")		  		End Sub	#tag EndMethod	#tag Method, Flags = &h0		Sub Constructor(theErrorType as String, theMessage as String, theWarningLevel as integer)		  me.errorType = theErrorType		  me.message = theMessage		  me.warningLevel = theWarningLevel		  		  me.warningLevelMessage = array("Error","Warning")		  		End Sub	#tag EndMethod	#tag Method, Flags = &h0		Function getErrorType() As String		  return me.errorType		  		End Function	#tag EndMethod	#tag Method, Flags = &h0		Function getMessage() As String		  return me.message		  		End Function	#tag EndMethod	#tag Method, Flags = &h0		Function getWarningLevel() As Integer		  return me.warningLevel		  		  		End Function	#tag EndMethod	#tag Method, Flags = &h0		Function toString() As String		  return me.warningLevelMessage(me.warningLevel) + TAB + me.errorType + TAB + me.message		  		End Function	#tag EndMethod	#tag Property, Flags = &h1		Protected errorType As string	#tag EndProperty	#tag Property, Flags = &h1		Protected message As string	#tag EndProperty	#tag Property, Flags = &h1		Protected warningLevel As Integer = 0	#tag EndProperty	#tag Property, Flags = &h0		warningLevelMessage(-1) As string	#tag EndProperty	#tag ViewBehavior		#tag ViewProperty			Name="Index"			Visible=true			Group="ID"			InitialValue="-2147483648"			InheritedFrom="Object"		#tag EndViewProperty		#tag ViewProperty			Name="Left"			Visible=true			Group="Position"			InitialValue="0"			InheritedFrom="Object"		#tag EndViewProperty		#tag ViewProperty			Name="Name"			Visible=true			Group="ID"			InheritedFrom="Object"		#tag EndViewProperty		#tag ViewProperty			Name="Super"			Visible=true			Group="ID"			InheritedFrom="Object"		#tag EndViewProperty		#tag ViewProperty			Name="Top"			Visible=true			Group="Position"			InitialValue="0"			InheritedFrom="Object"		#tag EndViewProperty	#tag EndViewBehaviorEnd Class#tag EndClass
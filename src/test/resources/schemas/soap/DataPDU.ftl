<Saa:DataPDU xmlns:Saa="urn:swift:saa:xsd:saa.2.0">
	<Saa:Revision>2.0.2</Saa:Revision>
	<Saa:Header>
		<Saa:Message>
			<Saa:SenderReference>${senderReference}</Saa:SenderReference> <#--REF10812031316-->
			<Saa:MessageIdentifier>${messageIdentifier}</Saa:MessageIdentifier> <#--fin.999-->
			<Saa:Format>${format}</Saa:Format> <#--MT-->
			<Saa:Sender>
				<Saa:BIC12>${senderBIC}</Saa:BIC12> <#--SAASBEBBAXXX-->
				<Saa:FullName>
					<Saa:X1>${senderBICFullName}</Saa:X1> <#--SAASBEBBXXX-->
				</Saa:FullName>
			</Saa:Sender>
			<Saa:Receiver>
				<Saa:BIC12>${receiverBIC}</Saa:BIC12> <#--SAATBEBBXXXX-->
				<Saa:FullName>
					<Saa:X1>${receiverBICFullName}</Saa:X1> <#--SAATBEBBXXX-->
				</Saa:FullName>
			</Saa:Receiver>
			<Saa:InterfaceInfo>
				<Saa:UserReference>${userReference}</Saa:UserReference> <#--REF10812031316-->
			</Saa:InterfaceInfo>
			<Saa:NetworkInfo>
				<Saa:FINNetworkInfo />
			</Saa:NetworkInfo>
			<Saa:SecurityInfo>
				<Saa:FINSecurityInfo />
			</Saa:SecurityInfo>
		</Saa:Message>
	</Saa:Header>
	<Saa:Body>${base64Body}</Saa:Body> <#--DQo6MjA6VFJOIEZUSTAwMA0KOjc5OlpaWlpaWlpaWlpaWlpaWlpaWlpa-->
</Saa:DataPDU>
<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/" xmlns:urn="urn:swift:saa:xsd:soapha">
	<S:Header>
		<urn:SAAHeader Id="SAAHeader">
			<urn:SessionToken>${sessionToken}</urn:SessionToken>
			<urn:ClientRef>${clientReference}</urn:ClientRef> <#--REF1-->
		</urn:SAAHeader>
	</S:Header>
	<S:Body>
		<urn:GetAck />
	</S:Body>
</S:Envelope>
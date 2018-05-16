<S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/" xmlns:urn="urn:swift:saa:xsd:soapha">
	<S:Header />
	<S:Body>
		<urn:Open>
			<urn:MessagePartnerName>${messagePartner}</urn:MessagePartnerName>
			<urn:SequenceNumberToSAA>${sequenceNumber}</urn:SequenceNumberToSAA>
			<urn:WindowSize>${windowSize}</urn:WindowSize>
			<urn:FlowDirection>${flowDirection}</urn:FlowDirection>
		</urn:Open>
	</S:Body>
</S:Envelope>
package scott.barleydb.api.graphql;

public interface GraphQLContext {

	public <T> T execute(String body);

}
